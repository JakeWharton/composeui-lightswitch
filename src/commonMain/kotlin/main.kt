import kotlinx.cinterop.get
import kotlinx.cinterop.pointed
import lightswitch.DRM_MODE_CONNECTED
import lightswitch.drmModeConnectorPtr
import lightswitch.drmModeEncoderPtr
import lightswitch.drmModeFreeConnector
import lightswitch.drmModeFreeEncoder
import lightswitch.drmModeFreeResources
import lightswitch.drmModeGetConnector
import lightswitch.drmModeGetCrtc
import lightswitch.drmModeGetEncoder
import lightswitch.drmModeGetResources
import lightswitch.drmModeResPtr
import platform.posix.O_CLOEXEC
import platform.posix.O_RDWR
import platform.posix.close
import platform.posix.open

private const val devicePath = "/dev/dri/card0"

fun main() {
	println("Hello!")

	val deviceFd = open(devicePath, O_RDWR or O_CLOEXEC)
	check(deviceFd != -1) {
		"Couldn't open $devicePath"
	}
	println("Got DRM device $deviceFd")

	try {
		val resourcesPtr = checkNotNull(drmModeGetResources(deviceFd)) {
			"Couldn't get resources"
		}
		println("Got DRM resources")
		try {
			val connectorPtr = checkNotNull(findConnector(deviceFd, resourcesPtr)) {
				"Couldn't find any connectors"
			}
			try {
				connectorPtr.pointed.modes!![0].let { mode ->
					println("Resolution ${mode.vdisplay}x${mode.hdisplay}")
				}

				val encoderPtr = checkNotNull(findEncoder(deviceFd, resourcesPtr, connectorPtr)) {
					"Couldn't find any encoders"
				}
				println("Got DRM encoder")
				try {
					val crtcId = encoderPtr.pointed.crtc_id
					// The Flutter embedded code does another loop here if the CRTC ID is missing. I'm not
					// entirely convinced that's needed, so guard the condition for now.
					check(crtcId != 0U) { "Encoder has no CTRC ID!" }

					val crtcPtr = checkNotNull(drmModeGetCrtc(deviceFd, crtcId)) {
						"Couldn't find a suitable CRTC"
					}
					println("Got CRTC for ID $crtcId")
					println("All good!")
				} finally {
					drmModeFreeEncoder(encoderPtr)
				}
			} finally {
				drmModeFreeConnector(connectorPtr)
			}
		} finally {
			drmModeFreeResources(resourcesPtr)
		}
	} finally {
		close(deviceFd)
	}
}

private fun findConnector(
	deviceFd: Int,
	resourcesPtr: drmModeResPtr,
): drmModeConnectorPtr? {
	val resources = resourcesPtr.pointed
	for (i in 0 until resources.count_connectors) {
		val connectorPtr = drmModeGetConnector(deviceFd, resources.connectors!![i]) ?: continue
		if (connectorPtr.pointed.connection == DRM_MODE_CONNECTED) {
			return connectorPtr
		}
		drmModeFreeConnector(connectorPtr)
	}
	return null
}

private fun findEncoder(
	deviceFd: Int,
	resourcesPtr: drmModeResPtr,
	connectorPtr: drmModeConnectorPtr,
): drmModeEncoderPtr? {
	val resources = resourcesPtr.pointed
	val connector = connectorPtr.pointed
	for (i in 0 until resources.count_encoders) {
		val encoderPtr = drmModeGetEncoder(deviceFd, resources.encoders!![i]) ?: continue
		val encoder = encoderPtr.pointed
		for (j in 0 until connector.count_encoders) {
			if (encoder.encoder_id == connector.encoders!![j]) {
				return encoderPtr
			}
		}
		drmModeFreeEncoder(encoderPtr)
	}

	// If encoder is not connected to the connector, try to find a suitable one.
	for (i in 0 until connector.count_encoders) {
		val encoderPtr = drmModeGetEncoder(deviceFd, connector.encoders!![i]) ?: continue
		val encoder = encoderPtr.pointed
		for (j in 0 until resources.count_crtcs) {
			if (encoder.possible_crtcs and (1U shl j) != 0U) {
				encoder.crtc_id = resources.crtcs!![j]
				return encoderPtr
			}
		}
		drmModeFreeEncoder(encoderPtr)
	}

	return null
}
