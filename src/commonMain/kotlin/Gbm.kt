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
			val devicePtr = gbm_create_device(drm.fd)
				.checkNotNull { "Couldn't create the GBM device" }
				.scopedUseWithClose("Destroying GBM device", ::gbm_device_destroy)
			println("Got GBM device")

			val surfacePtr = drm.modeInfo
				.useContents {
					gbm_surface_create(
						devicePtr,
						hdisplay.convert(),
						vdisplay.convert(),
						GBM_FORMAT_XRGB8888,
						GBM_BO_USE_SCANOUT or GBM_BO_USE_RENDERING,
					)
				}
				.checkNotNull { "Failed to create the GBM surface" }
				.scopedUseWithClose("Destroying GBM surface", ::gbm_surface_destroy)
			println("Created GBM surface")

			return Gbm(
				closer = closer,
				devicePtr = devicePtr,
				surfacePtr = surfacePtr,
			)
		}
	}
}
