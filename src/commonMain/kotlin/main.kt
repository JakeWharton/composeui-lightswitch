import kotlinx.cinterop.useContents

private const val devicePath = "/dev/dri/card0"

fun main() {
	println("Hello!")

	NativeWindowDrm.initialize(devicePath).use {
		it.modeInfo.useContents {
			println("Resolution: ${vdisplay}x$hdisplay")
		}
	}

	println("All done!")
}
