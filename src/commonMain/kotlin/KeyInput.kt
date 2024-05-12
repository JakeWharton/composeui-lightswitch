import androidx.compose.ui.input.key.Key.Companion.Button1
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType.Companion.KeyDown
import androidx.compose.ui.input.key.KeyEventType.Companion.KeyUp
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.toKString
import lightswitch.EV_KEY
import lightswitch.KEY_F2
import lightswitch.eviocgname
import lightswitch.input_event
import platform.linux.char16_tVar
import platform.posix.O_NONBLOCK
import platform.posix.O_RDONLY
import platform.posix.close
import platform.posix.ioctl
import platform.posix.open

internal class KeyInput(
	val fd: Int,
) {
	fun process(event: input_event): KeyEvent? {
		when (event.type.convert<Int>()) {
			EV_KEY -> {
				when (event.code.convert<Int>()) {
					KEY_F2 -> {
						when (event.value) {
							0 -> return KeyEvent(Button1, KeyUp)
							1 -> return KeyEvent(Button1, KeyDown)
						}
					}
				}
			}
		}
		return null
	}
}

internal fun CloserScope.openKeyInputDevice(path: String): KeyInput {
	val deviceFd = open(path, O_RDONLY or O_NONBLOCK)
		.checkReturn { "Couldn't open $path" }
		.scopedUseWithClose("Closing key input device", ::close)

	memScoped {
		val nameBufferSize = 256
		val name = allocArray<char16_tVar>(nameBufferSize)
		ioctl(deviceFd, eviocgname(nameBufferSize).toULong(), name)
		println(
			"""
			|Opened key input device (fd: $deviceFd):
			| - device: $path
			| - name: ${name.toKString()}
			""".trimMargin(),
		)
	}

	return KeyInput(deviceFd)
}
