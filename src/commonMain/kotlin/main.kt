import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.LocalSystemTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.SystemTheme
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeCanvas
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.platform.ComposeUiMainDispatcher
import androidx.compose.ui.scene.ComposeScene
import androidx.compose.ui.scene.MultiLayerComposeScene
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.random.Random
import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.StableRef
import kotlinx.cinterop.alloc
import kotlinx.cinterop.asStableRef
import kotlinx.cinterop.cValuesOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.pointed
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.sizeOf
import kotlinx.cinterop.staticCFunction
import kotlinx.cinterop.toKString
import kotlinx.cinterop.value
import lightswitch.ABS_MT_POSITION_X
import lightswitch.ABS_MT_POSITION_Y
import lightswitch.BTN_TOUCH
import lightswitch.DRM_EVENT_CONTEXT_VERSION
import lightswitch.DRM_MODE_PAGE_FLIP_EVENT
import lightswitch.EV_ABS
import lightswitch.EV_KEY
import lightswitch.EV_SYN
import lightswitch.GL_COLOR_BUFFER_BIT
import lightswitch.drmEventContext
import lightswitch.drmHandleEvent
import lightswitch.drmModeAddFB
import lightswitch.drmModePageFlip
import lightswitch.drmModeRmFB
import lightswitch.drmModeSetCrtc
import lightswitch.drm_fb
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
import lightswitch.input_event
import lightswitch.select_fd_isset
import lightswitch.select_fd_set
import lightswitch.select_fd_zero
import org.jetbrains.skia.BackendRenderTarget
import org.jetbrains.skia.Color as SkiaColor
import org.jetbrains.skia.ColorSpace
import org.jetbrains.skia.DirectContext
import org.jetbrains.skia.FramebufferFormat
import org.jetbrains.skia.Surface
import org.jetbrains.skia.SurfaceColorFormat
import org.jetbrains.skia.SurfaceOrigin
import org.jetbrains.skia.SurfaceProps
import org.jetbrains.skiko.currentNanoTime
import platform.posix.FD_SETSIZE
import platform.posix.calloc
import platform.posix.errno
import platform.posix.fd_set
import platform.posix.free
import platform.posix.read
import platform.posix.select
import platform.posix.strerror
import platform.posix.uint32_tVar

private const val RENDER_DEVICE = "/dev/dri/card0"
private const val TOUCH_DEVICE = "/dev/input/event1"
private const val KEY_DEVICE = "/dev/input/event3"

private class State(
	val drm: Drm,
	val gbm: Gbm,
	val egl: Egl,
	val context: DirectContext,
	var thisBo: CPointer<gbm_bo>,
	var lastBo: CPointer<gbm_bo>?,
	val scene: ComposeScene,
)

fun main() = closeFinallyScope {
	memScoped {
		val touch = TouchInput.initialize(TOUCH_DEVICE).useInScope()
		val keys = KeyInput.initialize(KEY_DEVICE).useInScope()
		val drm = Drm.initialize(RENDER_DEVICE).useInScope()
		val gbm = Gbm.initialize(drm).useInScope()
		val egl = Egl.initialize(gbm).useInScope()

		val width: Int
		val height: Int
		drm.modeInfo.useContents {
			width = hdisplay.convert()
			height = vdisplay.convert()
		}

		val scene = MultiLayerComposeScene(
			size = IntSize(
				width = width,
				height = height,
			),
			coroutineContext = ComposeUiMainDispatcher,
		)
		scene.setContent {
			CompositionLocalProvider(LocalSystemTheme provides SystemTheme.Dark) {
				val initialColor = MaterialTheme.colorScheme.surface
				var color by remember { mutableStateOf(initialColor) }
				Button(
					onClick = {
						color = Color(Random.nextInt(0xFFFFFF) or 0xFF000000.toInt())
					},
					colors = ButtonDefaults.buttonColors(containerColor = color),
					modifier = Modifier.padding(16.dp).fillMaxSize(),
				) {}
			}
		}

		glClearColor(0.5f, 0.5f, 0.5f, 1.0f)
		glClear(GL_COLOR_BUFFER_BIT.toUInt())
		eglSwapBuffers(egl.display, egl.surface)

		val firstBo = checkNotNull(gbm_surface_lock_front_buffer(gbm.surfacePtr)) {
			"Unable to lock first GBM surface front buffer"
		}
		println("Locked first GBM surface front buffer")

		val firstFb = drmFbGetFromBo(drm, firstBo)
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
		eventContext.page_flip_handler = staticCFunction(::pageFlipHandler)

		val context = DirectContext.makeEGL()
			.scopedUseWithClose("Closing EGL context", DirectContext::close)

		val state = State(
			drm = drm,
			gbm = gbm,
			egl = egl,
			context = context,
			thisBo = firstBo,
			lastBo = null,
			scene = scene,
		)
		closer += {
			state.lastBo?.let { lastBo ->
				println("Releasing last GBM surface buffer")
				gbm_surface_release_buffer(gbm.surfacePtr, lastBo)
			}
		}

		val statePtr = StableRef.create(state).useInScope().asCPointer()
		renderFrame(drm.fd, statePtr, state)

		val event = alloc<input_event>()
		val eventSize = sizeOf<input_event>()

		val fds = alloc<fd_set>()
		select_fd_zero(fds.ptr)

		var isButtonDown = false
		var nextButtonUp = false
		var nextX = 0
		var nextY = 0

		while (true) {
			select_fd_set(drm.fd, fds.ptr)
			select_fd_set(touch.fd, fds.ptr)
			select_fd_set(keys.fd, fds.ptr)
			select(FD_SETSIZE, fds.ptr, null, null, null).let { result ->
				check(result >= 0) { "select error: " + strerror(errno)?.toKString() }
				check(result != 0) { "select timeout" }
			}

			if (select_fd_isset(touch.fd, fds.ptr) != 0) {
				val size = read(touch.fd, event.ptr, eventSize.convert())
				check(size == eventSize) {
					"Touch FD read too few bytes: $size < $eventSize"
				}
				println("Touch! type: ${event.type}, code: ${event.code}, value: ${event.value}, time: ${event.time.tv_sec}.${event.time.tv_usec}")
				when (event.type.convert<Int>()) {
					EV_ABS -> {
						when (event.code.convert<Int>()) {
							ABS_MT_POSITION_X -> {
								nextX = event.value
							}
							ABS_MT_POSITION_Y -> {
								nextY = event.value
							}
						}
					}
					EV_KEY -> {
						when (event.code.convert<Int>()) {
							BTN_TOUCH -> {
								when (event.value) {
									0 -> {
										nextButtonUp = true
									}
								}
							}
						}
					}
					EV_SYN -> {
						when (event.code.convert<Int>()) {
							0 -> {
								val eventType = when {
									!isButtonDown -> PointerEventType.Press
									nextButtonUp -> PointerEventType.Release
									else -> PointerEventType.Move
								}
								val position = Offset(nextX.toFloat(), nextY.toFloat())
								val timeMillis = event.time.tv_sec * 1000 + event.time.tv_usec / 1000
								println("Send $eventType at $position")
								scene.sendPointerEvent(
									eventType = eventType,
									position = position,
									type = PointerType.Touch,
									timeMillis = timeMillis,
								)
								isButtonDown = !nextButtonUp
								nextButtonUp = false
							}
						}
					}
				}
			}

			if (select_fd_isset(keys.fd, fds.ptr) != 0) {
				val size = read(keys.fd, event.ptr, eventSize.convert())
				check(size == eventSize) {
					"Key FD read too few bytes: $size < $eventSize"
				}
				println("Key! type: ${event.type}, code: ${event.code}, value: ${event.value}, time: ${event.time.tv_sec}.${event.time.tv_usec}")
			}

			if (select_fd_isset(drm.fd, fds.ptr) != 0) {
				drmHandleEvent(drm.fd, eventContext.ptr)
			}
		}
	}
}

@Suppress("UNUSED_PARAMETER") // Signature required for DRM.
private fun pageFlipHandler(fd: Int, frame: UInt, sec: UInt, usec: UInt, data: COpaquePointer?) {
	println("Page flip!")

	val state = data!!.asStableRef<State>().get()
	renderFrame(fd, data, state)
}

private fun renderFrame(fd: Int, data: COpaquePointer, state: State) {
	println("Render frame!!!")

	state.lastBo?.let { lastBo ->
		gbm_surface_release_buffer(state.gbm.surfacePtr, lastBo)
	}

	draw(state)

	eglSwapBuffers(state.egl.display, state.egl.surface)
	val nextBo = checkNotNull(gbm_surface_lock_front_buffer(state.gbm.surfacePtr)) {
		"Unable to lock next GBM surface front buffer"
	}
	println("Locked next GBM surface front buffer")

	val nextFb = drmFbGetFromBo(state.drm, nextBo)

	drmModePageFlip(
		fd = fd,
		crtc_id = state.drm.crtcId,
		fb_id = nextFb.pointed.fb_id,
		flags = DRM_MODE_PAGE_FLIP_EVENT.toUInt(),
		user_data = data,
	).let { result ->
		check(result == 0) {
			"Failed to queue page flip: " + strerror(errno)?.toKString()
		}
	}
	println("Queued page flip.")

	state.lastBo = state.thisBo
	state.thisBo = nextBo
}

private fun draw(state: State) {
	val width: Int
	val height: Int
	state.drm.modeInfo.useContents {
		width = hdisplay.convert()
		height = vdisplay.convert()
	}

	val renderTarget = BackendRenderTarget.makeGL(
		width = width,
		height = height,
		sampleCnt = 0,
		stencilBits = 8,
		fbId = 0,
		fbFormat = FramebufferFormat.GR_GL_RGBA8,
	)

	val surface = Surface.makeFromBackendRenderTarget(
		state.context,
		renderTarget,
		SurfaceOrigin.BOTTOM_LEFT,
		SurfaceColorFormat.RGBA_8888,
		ColorSpace.sRGB,
		SurfaceProps(),
	) ?: throw IllegalStateException("Cannot create surface")

	val canvas = surface.canvas

	canvas.clear(SkiaColor.BLACK)
	state.scene.render(canvas.asComposeCanvas(), currentNanoTime())

	surface.flushAndSubmit()
	surface.close()
	renderTarget.close()
}

private fun drmFbGetFromBo(drm: Drm, bo: CPointer<gbm_bo>): CPointer<drm_fb> {
	val userDataPtr = gbm_bo_get_user_data(bo)
	if (userDataPtr != null) {
		return userDataPtr.reinterpret()
	}

	val drmFbSize = sizeOf<drm_fb>().toULong()
	val fbPtr = checkNotNull(calloc(1U, drmFbSize)) {
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

	gbm_bo_set_user_data(bo, fbPtr, staticCFunction(::drmFbDestroyCallback))

	return fbPtr
}

private fun drmFbDestroyCallback(bo: CPointer<gbm_bo>?, data: COpaquePointer?) {
	val fb = data!!.reinterpret<drm_fb>()
	val fbId = fb.pointed.fb_id
	if (fbId != 0U) {
		val gbm = gbm_bo_get_device(bo)!!
		val fd = gbm_device_get_fd(gbm)
		drmModeRmFB(fd, fbId)
	}

	free(fb)
}
