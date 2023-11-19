import kotlinx.cinterop.CPointer
import kotlinx.cinterop.alloc
import kotlinx.cinterop.cValuesOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.pointed
import kotlinx.cinterop.ptr
import kotlinx.cinterop.useContents
import kotlinx.cinterop.value
import lightswitch.GBM_BO_USE_RENDERING
import lightswitch.GBM_BO_USE_SCANOUT
import lightswitch.GBM_FORMAT_ARGB8888
import lightswitch.drmModeAddFB
import lightswitch.drmModeRmFB
import lightswitch.drmModeSetCrtc
import lightswitch.gbm_bo
import lightswitch.gbm_bo_get_handle
import lightswitch.gbm_bo_get_height
import lightswitch.gbm_bo_get_stride
import lightswitch.gbm_bo_get_width
import lightswitch.gbm_create_device
import lightswitch.gbm_device_destroy
import lightswitch.gbm_surface
import lightswitch.gbm_surface_create
import lightswitch.gbm_surface_destroy
import lightswitch.gbm_surface_lock_front_buffer
import lightswitch.gbm_surface_release_buffer
import platform.posix.uint32_t
import platform.posix.uint32_tVar

internal class NativeWindowDrmGbm(
	val nativeWindowDrm: NativeWindowDrm,
	val onscreenSurface: CPointer<gbm_surface>,
	val offscreenSurface: CPointer<gbm_surface>,
	private val closer: Closer,
) : AutoCloseable by closer {
	private var previousBo: CPointer<gbm_bo>? = null
	private var previousFb: uint32_t = 0U

	fun swapBuffers() = memScoped {
		val bo = gbm_surface_lock_front_buffer(onscreenSurface)
		val width = gbm_bo_get_width(bo)
		val height = gbm_bo_get_height(bo)
		val handle = gbm_bo_get_handle(bo).useContents { u32 }
		val stride = gbm_bo_get_stride(bo)

		val fb = alloc<uint32_tVar>()
		val fbResult = drmModeAddFB(
			fd = nativeWindowDrm.deviceFd,
			width = width,
			height = height,
			depth = 24U,
			bpp = 32U,
			pitch = stride,
			bo_handle = handle,
			buf_id = fb.ptr,
		)
		if (fbResult != 0) {
			println("Failed to add frame buffer ($fbResult)")
		}

		val crtc = nativeWindowDrm.crtcPtr.pointed
		val crtcResult = drmModeSetCrtc(
			fd = nativeWindowDrm.deviceFd,
			crtcId = crtc.crtc_id,
			bufferId = fb.value,
			x = 0U,
			y = 0U,
			connectors = cValuesOf(nativeWindowDrm.connectorId),
			count = 1,
			mode = nativeWindowDrm.modeInfo.ptr,
		)
		if (crtcResult != 0) {
			println("Failed to set crct mode ($crtcResult)")
		}

		previousBo?.let { previousBo ->
			drmModeRmFB(nativeWindowDrm.deviceFd, previousFb)
			gbm_surface_release_buffer(onscreenSurface, previousBo)
		}

		previousBo = bo
		previousFb = fb.value
	}

	companion object {
		fun initialize(devicePath: String): NativeWindowDrmGbm = closeOnThrowScope {
			val nativeWindowDrm = NativeWindowDrm.initialize(devicePath).useInScope()

			// Note: drmIsMaster check removed because unsupported on the target.
			// if (drmIsMaster(nativeWindowDrm.deviceFd) == 0) { .. }

			val gbmDevice = checkNotNull(gbm_create_device(nativeWindowDrm.deviceFd)) {
				"Couldn't create the GBM device"
			}
			closer += {
				println("Destroying GBM device")
				gbm_device_destroy(gbmDevice)
			}
			println("Got GBM device")

			val gbmOnscreenSurface = checkNotNull(nativeWindowDrm.modeInfo.useContents {
				gbm_surface_create(
					gbmDevice,
					hdisplay.convert(),
					vdisplay.convert(),
					GBM_FORMAT_ARGB8888,
					GBM_BO_USE_SCANOUT or GBM_BO_USE_RENDERING,
				)
			}) {
				"Failed to create the onscreen GBM surface"
			}
			closer += {
				println("Destroying onscreen GBM surface")
				gbm_surface_destroy(gbmOnscreenSurface)
			}
			println("Created onscreen GBM surface")

			val gbmOffscreenSurface = checkNotNull(nativeWindowDrm.modeInfo.useContents {
				gbm_surface_create(
					gbmDevice,
					1U,
					1U,
					GBM_FORMAT_ARGB8888,
					GBM_BO_USE_SCANOUT or GBM_BO_USE_RENDERING,
				)
			}) {
				"Failed to create the offscreen GBM surface"
			}
			closer += {
				println("Destroying offscreen GBM surface")
				gbm_surface_destroy(gbmOffscreenSurface)
			}
			println("Created offscreen GBM surface")

			return NativeWindowDrmGbm(
				nativeWindowDrm = nativeWindowDrm,
				onscreenSurface = gbmOnscreenSurface,
				offscreenSurface = gbmOffscreenSurface,
				closer = closer,
			)
		}
	}
}
