import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.CFunction
import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.CValuesRef
import kotlinx.cinterop.alloc
import kotlinx.cinterop.cValuesOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.invoke
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.toKString
import kotlinx.cinterop.value
import lightswitch.EGLConfig
import lightswitch.EGLConfigVar
import lightswitch.EGLContext
import lightswitch.EGLDisplay
import lightswitch.EGLSurface
import lightswitch.EGL_ALPHA_SIZE
import lightswitch.EGL_BLUE_SIZE
import lightswitch.EGL_CONTEXT_CLIENT_VERSION
import lightswitch.EGL_EXTENSIONS
import lightswitch.EGL_GREEN_SIZE
import lightswitch.EGL_NONE
import lightswitch.EGL_NO_CONTEXT
import lightswitch.EGL_OPENGL_ES2_BIT
import lightswitch.EGL_OPENGL_ES_API
import lightswitch.EGL_PLATFORM_GBM_KHR
import lightswitch.EGL_RED_SIZE
import lightswitch.EGL_RENDERABLE_TYPE
import lightswitch.EGL_SURFACE_TYPE
import lightswitch.EGL_TRUE
import lightswitch.EGL_VENDOR
import lightswitch.EGL_VERSION
import lightswitch.EGL_WINDOW_BIT
import lightswitch.EGLenum
import lightswitch.EGLintVar
import lightswitch.GL_EXTENSIONS
import lightswitch.eglBindAPI
import lightswitch.eglChooseConfig
import lightswitch.eglCreateContext
import lightswitch.eglCreateWindowSurface
import lightswitch.eglDestroyContext
import lightswitch.eglDestroySurface
import lightswitch.eglGetProcAddress
import lightswitch.eglInitialize
import lightswitch.eglMakeCurrent
import lightswitch.eglQueryString
import lightswitch.eglTerminate
import lightswitch.glGetString

internal class Gl private constructor(
	private val closer: Closer,
	val display: EGLDisplay,
	val config: EGLConfig,
	val context: EGLContext,
	val surface: EGLSurface,
) : AutoCloseable by closer {
	companion object {
		fun initialize(gbm: Gbm): Gl = closeOnThrowScope {
			memScoped {
				@Suppress("UNCHECKED_CAST") // Types from https://registry.khronos.org/EGL/extensions/EXT/EGL_EXT_platform_base.txt
				val getPlatformDisplay = checkNotNull(eglGetProcAddress("eglGetPlatformDisplayEXT")) {
					"Unable to get eglGetPlatformDisplayEXT function pointer"
				} as CPointer<CFunction<(EGLenum, COpaquePointer, CValuesRef<EGLintVar>?) -> EGLDisplay?>>

				val display = checkNotNull(getPlatformDisplay.invoke(EGL_PLATFORM_GBM_KHR.toUInt(), gbm.devicePtr, null)) {
					"Unable to get EGL display"
				}
				println("Got EGL display")

				val major = alloc<EGLintVar>()
				val minor = alloc<EGLintVar>()
				check(eglInitialize(display, major.ptr, minor.ptr) == EGL_TRUE.toUInt()) {
					"Unable to initialize EGL display"
				}
				println("Initialized EGL display")

				println("Using display ${display.rawValue} with EGL version ${major.value}.${minor.value}")
				println("EGL Version " + eglQueryString(display, EGL_VERSION)?.toKString())
				println("EGL Vendor " + eglQueryString(display, EGL_VENDOR)?.toKString())
				println("EGL Extensions " + eglQueryString(display, EGL_EXTENSIONS)?.toKString())
				closer += {
					println("Terminating EGL display")
					eglTerminate(display)
				}

				check(eglBindAPI(EGL_OPENGL_ES_API.toUInt()) == EGL_TRUE.toUInt()) {
					"Failed to bind EGL_OPENGL_ES_API"
				}
				println("Bound OpenGL ES API")

				val configAttribs = cValuesOf(
					EGL_SURFACE_TYPE, EGL_WINDOW_BIT,
					EGL_RED_SIZE, 1,
					EGL_GREEN_SIZE, 1,
					EGL_BLUE_SIZE, 1,
					EGL_ALPHA_SIZE, 0,
					EGL_RENDERABLE_TYPE, EGL_OPENGL_ES2_BIT,
					EGL_NONE
				)
				val configVar = alloc<EGLConfigVar>()
				val configNumber = alloc<EGLintVar>()
				check(eglChooseConfig(display, configAttribs.ptr, configVar.ptr, 1.convert(), configNumber.ptr) == EGL_TRUE.toUInt() && configNumber.value == 1) {
					"Failed to choose EGL config: ${configNumber.value}"
				}
				println("Chose EGL config: ${configNumber.value}")
				val config = checkNotNull(configVar.value) {
					"Failed to obtain EGL config"
				}

				val contextAttribs = cValuesOf(
						EGL_CONTEXT_CLIENT_VERSION, 2,
						EGL_NONE
				)
				val context = checkNotNull(eglCreateContext(display, config, EGL_NO_CONTEXT, contextAttribs.ptr)) {
					"Failed to create EGL context"
				}
				closer += {
					println("Destroying EGL context")
					eglDestroyContext(display, context)
				}
				println("Created EGL context")

				val surface = checkNotNull(eglCreateWindowSurface(display, config, gbm.surfacePtr.rawValue.toLong().toULong(), null)) {
					"Failed to create EGL surface"
				}
				closer += {
					println("Destroying EGL surface")
					eglDestroySurface(display, surface)
				}
				println("Created EGL surface")

				eglMakeCurrent(display, surface, surface, context)
				println("Made EGL surface current")

				println("GL Extensions: " + glGetString(GL_EXTENSIONS.toUInt())!!.reinterpret<ByteVar>().toKString());

				Gl(
					closer = closer,
					display = display,
					config = config,
					context = context,
					surface = surface,
				)
			}
		}
	}
}
