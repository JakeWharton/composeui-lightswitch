
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.toKString
import lightswitch.eviocgname
import platform.linux.char16_tVar
import platform.posix.O_NONBLOCK
import platform.posix.O_RDONLY
import platform.posix.close
import platform.posix.ioctl
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
			memScoped {
				val nameBufferSize = 256
				val name = allocArray<char16_tVar>(nameBufferSize)
				ioctl(deviceFd, eviocgname(nameBufferSize).toULong(), name)
				println(
					"""
					|Opened key input device (fd: $deviceFd):
					| - device: $devicePath
					| - name: ${name.toKString()}
					""".trimMargin()
				)
			}

			return KeyInput(
				deviceFd,
				closer,
			)
		}
	}
}
