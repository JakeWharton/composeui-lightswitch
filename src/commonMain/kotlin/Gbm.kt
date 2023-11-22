import kotlinx.cinterop.CPointer
import kotlinx.cinterop.convert
import kotlinx.cinterop.useContents
import lightswitch.GBM_BO_USE_RENDERING
import lightswitch.GBM_BO_USE_SCANOUT
import lightswitch.GBM_FORMAT_XRGB8888
import lightswitch.gbm_create_device
import lightswitch.gbm_device
import lightswitch.gbm_device_destroy
import lightswitch.gbm_surface
import lightswitch.gbm_surface_create
import lightswitch.gbm_surface_destroy

internal class Gbm private constructor(
	private val closer: Closer,
	val devicePtr: CPointer<gbm_device>,
	val surfacePtr: CPointer<gbm_surface>,
) : AutoCloseable by closer {
	companion object {
		fun initialize(drm: Drm): Gbm = closeOnThrowScope {
			val devicePtr = checkNotNull(gbm_create_device(drm.fd)) {
				"Couldn't create the GBM device"
			}
			closer += {
				println("Destroying GBM device")
				gbm_device_destroy(devicePtr)
			}
			println("Got GBM device")

			val surfacePtr = checkNotNull(drm.modeInfo.useContents {
				gbm_surface_create(
					devicePtr,
					hdisplay.convert(),
					vdisplay.convert(),
					GBM_FORMAT_XRGB8888,
					GBM_BO_USE_SCANOUT or GBM_BO_USE_RENDERING,
				)
			}) {
				"Failed to create the GBM surface"
			}
			closer += {
				gbm_surface_destroy(surfacePtr)
				println("Destroyed GBM surface")
			}
			println("Created GBM surface")

			return Gbm(
				closer = closer,
				devicePtr = devicePtr,
				surfacePtr = surfacePtr,
			)
		}
	}
}
