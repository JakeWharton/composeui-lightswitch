
import platform.posix.O_NONBLOCK
import platform.posix.O_RDONLY
import platform.posix.close
import platform.posix.open

internal class KeyInput private constructor(
	val fd: Int,
	private val closer: Closer,
) : AutoCloseable by closer {
	companion object {
		fun initialize(devicePath: String): KeyInput = closeOnThrowScope {
			val deviceFd = open(devicePath, O_RDONLY or O_NONBLOCK)
			check(deviceFd != -1) {
				"Couldn't open $devicePath"
			}
			closer += {
				println("Closing key input device")
				close(deviceFd)
			}
			println("Opened key input device (fd: $deviceFd)")

			return KeyInput(
				deviceFd,
				closer,
			)
		}
	}
}
