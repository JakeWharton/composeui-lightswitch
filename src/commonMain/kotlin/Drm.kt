import kotlinx.cinterop.CValue
import kotlinx.cinterop.cValuesOf
import kotlinx.cinterop.get
import kotlinx.cinterop.pointed
import kotlinx.cinterop.ptr
import kotlinx.cinterop.readValue
import kotlinx.cinterop.useContents
import lightswitch.DRM_MODE_CONNECTED
import lightswitch.drmModeConnectorPtr
import lightswitch.drmModeEncoderPtr
import lightswitch.drmModeFreeConnector
import lightswitch.drmModeFreeCrtc
import lightswitch.drmModeFreeEncoder
import lightswitch.drmModeFreeResources
import lightswitch.drmModeGetConnector
import lightswitch.drmModeGetCrtc
import lightswitch.drmModeGetEncoder
import lightswitch.drmModeGetResources
import lightswitch.drmModeModeInfo
import lightswitch.drmModeResPtr
import lightswitch.drmModeSetCrtc
import platform.posix.O_CLOEXEC
import platform.posix.O_RDWR
import platform.posix.close
import platform.posix.open
import platform.posix.uint32_t

internal class Drm private constructor(
	val fd: Int,
	val connectorId: UInt,
	val crtcId: UInt,
	val modeInfo: CValue<drmModeModeInfo>,
	private val closer: Closer,
) : AutoCloseable by closer {
	companion object {
		fun initialize(devicePath: String): Drm = closeOnThrowScope {
			val deviceFd = open(devicePath, O_RDWR or O_CLOEXEC)
				.checkReturn { "Couldn't open $devicePath" }
				.scopedUseWithClose("Closing DRM device", ::close)
			println("Opened DRM device (fd: $deviceFd)")

			val connectorId: uint32_t
			val modeInfo: CValue<drmModeModeInfo>
			val crtcId: uint32_t
			childCloseFinallyScope {
				val resourcesPtr = drmModeGetResources(deviceFd)
					.checkNotNull { "DRM resources" }
					.scopedUseWithClose("Freeing DRM resources", ::drmModeFreeResources)
				println("Got DRM resources")

				val connectorPtr = findConnector(deviceFd, resourcesPtr)
					.checkNotNull { "Couldn't find any connectors" }
					.scopedUseWithClose("Freeing DRM connector", ::drmModeFreeConnector)
				println("Got DRM connector")

				val encoderPtr = findEncoder(deviceFd, resourcesPtr, connectorPtr)
					.checkNotNull { "Couldn't find any encoders" }
					.scopedUseWithClose("Freeing DRM encoder", ::drmModeFreeEncoder)
				println("Got DRM encoder")

				val connector = connectorPtr.pointed
				connectorId = connector.connector_id
				modeInfo = connector.modes!![0].readValue()
				modeInfo.useContents {
					println("Resolution: ${hdisplay}x$vdisplay")
				}

				crtcId = encoderPtr.pointed.crtc_id
				// The Flutter embedded code does another loop here if the CRTC ID is missing. I'm not
				// entirely convinced that's needed, so guard the condition for now.
				check(crtcId != 0U) { "Encoder has no CTRC ID!" }
			}

			// TODO We don't use the return value. What's this doing?
			drmModeGetCrtc(deviceFd, crtcId)
				.checkNotNull { "Couldn't find a suitable CRTC" }
				.scopedUseWithClose("Freeing CRTC") { crtcPtr ->
					val crtc = crtcPtr.pointed
					drmModeSetCrtc(
						fd = deviceFd,
						crtcId = crtc.crtc_id,
						bufferId = crtc.buffer_id,
						x = crtc.x,
						y = crtc.y,
						connectors = cValuesOf(connectorId),
						count = 1,
						mode = crtc.mode.ptr,
					)
					drmModeFreeCrtc(crtcPtr)
				}
			println("Got CRTC for ID $crtcId")

			return Drm(
				fd = deviceFd,
				connectorId = connectorId,
				crtcId = crtcId,
				modeInfo = modeInfo,
				closer = closer,
			)
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
	}
}
