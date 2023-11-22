import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.IntVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.cValuesOf
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.pointed
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.sizeOf
import kotlinx.cinterop.staticCFunction
import kotlinx.cinterop.toKString
import kotlinx.cinterop.useContents
import kotlinx.cinterop.value
import lightswitch.DRM_EVENT_CONTEXT_VERSION
import lightswitch.DRM_MODE_PAGE_FLIP_EVENT
import lightswitch.GL_COLOR_BUFFER_BIT
import lightswitch.drmEventContext
import lightswitch.drmHandleEvent
import lightswitch.drm_fb
import lightswitch.drmModeAddFB
import lightswitch.drmModePageFlip
import lightswitch.drmModeRmFB
import lightswitch.drmModeSetCrtc
import lightswitch.eglSwapBuffers
import lightswitch.gbm_bo
import lightswitch.gbm_bo_get_device
import lightswitch.gbm_bo_get_handle
import lightswitch.gbm_bo_get_height
import lightswitch.gbm_bo_get_stride
import lightswitch.gbm_bo_get_user_data
import lightswitch.gbm_bo_get_width
import lightswitch.gbm_bo_set_user_data
import lightswitch.gbm_device_get_fd
import lightswitch.gbm_surface_lock_front_buffer
import lightswitch.gbm_surface_release_buffer
import lightswitch.glClear
import lightswitch.glClearColor
import lightswitch.select_fd_isset
import lightswitch.select_fd_set
import lightswitch.select_fd_zero
import platform.posix.calloc
import platform.posix.errno
import platform.posix.fd_set
import platform.posix.free
import platform.posix.select
import platform.posix.strerror
import platform.posix.uint32_tVar

const val devicePath = "/dev/dri/card0"

fun main() = closeFinallyScope {
	memScoped {
		val drm = Drm.initialize(devicePath).useInScope()

		val fds = alloc<fd_set>()
		select_fd_zero(fds.ptr)
		select_fd_set(0, fds.ptr)
		select_fd_set(drm.fd, fds.ptr)

		val gbm = Gbm.initialize(drm).useInScope()
		val gl = Gl.initialize(gbm).useInScope()

		glClearColor(0.5f, 0.5f, 0.5f, 1.0f)
		glClear(GL_COLOR_BUFFER_BIT.toUInt())
		eglSwapBuffers(gl.display, gl.surface)

		var bo = checkNotNull(gbm_surface_lock_front_buffer(gbm.surfacePtr)) {
			"Unable to lock GBM surface front buffer"
		}
		closer += {
			println("Releasing GBM surface buffer")
			gbm_surface_release_buffer(gbm.surfacePtr, bo)
		}
		println("Locked GBM surface front buffer")

		val firstFb = drm_fb_get_from_bo(drm, bo)
		drmModeSetCrtc(
			fd = drm.fd,
			crtcId = drm.crtcId,
			bufferId = firstFb.pointed.fb_id,
			x = 0U,
			y = 0U,
			connectors = cValuesOf(drm.connectorId),
			count = 1,
			mode = drm.modeInfo.ptr,
		).let { result ->
			check(result == 0) {
				"Failed to set mode: " + strerror(errno)?.toKString()
			}
		}
		println("Set CRTC")

		val eventContext = alloc<drmEventContext>()
		eventContext.version = DRM_EVENT_CONTEXT_VERSION
		eventContext.page_flip_handler = staticCFunction(::page_flip_handler)

		while (true) {
			println("Draw!!!")

			glClear(GL_COLOR_BUFFER_BIT.toUInt())
			glClearColor(0.2f, 0.3f, 0.5f, 1.0f)

			eglSwapBuffers(gl.display, gl.surface)
			val nextBo = checkNotNull(gbm_surface_lock_front_buffer(gbm.surfacePtr)) {
				"Unable to lock GBM surface front buffer"
			}
			println("Locked next GBM surface front buffer")

			val nextFb = drm_fb_get_from_bo(drm, nextBo)

			val waitForPageFlip = alloc<IntVar>()
			waitForPageFlip.value = 1

			drmModePageFlip(
				fd = drm.fd,
				crtc_id = drm.crtcId,
				fb_id = nextFb.pointed.fb_id,
				flags = DRM_MODE_PAGE_FLIP_EVENT.toUInt(),
				user_data = waitForPageFlip.ptr,
			).let { result ->
				check(result == 0) {
					"Failed to queue page flip: " + strerror(errno)?.toKString()
				}
			}
			println("Queued page flip.")

			while (waitForPageFlip.value != 0) {
				val result = select(drm.fd + 1, fds.ptr, null, null, null)
				check(result >= 0) {
					"select error: " + strerror(errno)?.toKString()
				}
				check(result != 0) {
					"select timeout"
				}
				if (select_fd_isset(0, fds.ptr) != 0) {
					println("User interrupted!")
					break
				}
				drmHandleEvent(drm.fd, eventContext.ptr)
			}

			gbm_surface_release_buffer(gbm.surfacePtr, bo);
			bo = nextBo;
		}
	}
}

private fun page_flip_handler(fd: Int, frame: UInt, sec: UInt, usec: UInt, data: COpaquePointer?) {
	data!!.reinterpret<IntVar>().pointed.value = 0
}

private fun drm_fb_get_from_bo(drm: Drm, bo: CPointer<gbm_bo>): CPointer<drm_fb> {
	val userDataPtr = gbm_bo_get_user_data(bo)
	if (userDataPtr != null) {
		return userDataPtr.reinterpret()
	}

	val drmFbSize = sizeOf<drm_fb>().toULong()
	val fbPtr = checkNotNull( calloc(1U, drmFbSize)) {
		"Unable to allocate $drmFbSize bytes for drm_fb"
	}.reinterpret<drm_fb>()
	val fb = fbPtr.pointed
	fb.bo = bo

	val width = gbm_bo_get_width(bo)
	val height = gbm_bo_get_height(bo)
	val stride = gbm_bo_get_stride(bo)
	val handle = gbm_bo_get_handle(bo).useContents { u32 }

	memScoped {
		val fbId = alloc<uint32_tVar>()
		val result = drmModeAddFB(drm.fd, width, height, 24U, 32U, stride, handle, fbId.ptr)
		fb.fb_id = fbId.value
		check(result == 0) {
			"Failed to create FB: " + strerror(errno)?.toKString()
		}
	}

	gbm_bo_set_user_data(bo, fbPtr, staticCFunction(::drm_fb_destroy_callback))

	return fbPtr
}

private fun drm_fb_destroy_callback(bo: CPointer<gbm_bo>?, data: COpaquePointer?) {
	val fb = data!!.reinterpret<drm_fb>()
	val fbId = fb.pointed.fb_id
	if (fbId != 0U) {
		val gbm = gbm_bo_get_device(bo)!!
		val fd = gbm_device_get_fd(gbm)
		drmModeRmFB(fd, fbId)
	}

	free(fb)
}
