
import platform.posix.O_NONBLOCK
import platform.posix.O_RDONLY
import platform.posix.close
import platform.posix.open

internal class TouchInput private constructor(
	val fd: Int,
	private val closer: Closer,
) : AutoCloseable by closer {
	companion object {
		fun initialize(devicePath: String): TouchInput = closeOnThrowScope {
			val deviceFd = open(devicePath, O_RDONLY or O_NONBLOCK)
			check(deviceFd != -1) {
				"Couldn't open $devicePath"
			}
			closer += {
				println("Closing touch input device")
				close(deviceFd)
			}
			println("Opened touch input device")

			return TouchInput(
				deviceFd,
				closer,
			)
		}
	}
}
