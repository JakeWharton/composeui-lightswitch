import kotlinx.cinterop.useContents

private const val devicePath = "/dev/dri/card0"

fun main() {
	println("Hello!")

	NativeWindowDrmGbm.initialize(devicePath).use { window ->
		window.nativeWindowDrm.modeInfo.useContents {
			println("Resolution: ${vdisplay}x$hdisplay")
		}
		repeat(5) {
			println("Swappin' buffers")
			window.swapBuffers()
		}
	}

	println("All done!")
}
