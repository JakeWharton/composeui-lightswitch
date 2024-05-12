import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventType
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.toKString
import lightswitch.ABS_MT_POSITION_X
import lightswitch.ABS_MT_POSITION_Y
import lightswitch.BTN_TOUCH
import lightswitch.EV_ABS
import lightswitch.EV_KEY
import lightswitch.EV_SYN
import lightswitch.eviocgname
import lightswitch.input_event
import platform.linux.char16_tVar
import platform.posix.O_NONBLOCK
import platform.posix.O_RDONLY
import platform.posix.close
import platform.posix.ioctl
import platform.posix.open

internal data class TouchEvent(
	val eventType: PointerEventType,
	val position: Offset,
	val timeMillis: Long,
)

internal class TouchInput(
	val fd: Int,
) {
	private var isButtonDown = false
	private var nextButtonUp = false
	private var nextX = 0
	private var nextY = 0

	fun process(event: input_event): TouchEvent? {
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

						isButtonDown = !nextButtonUp
						nextButtonUp = false

						return TouchEvent(
							eventType = eventType,
							position = position,
							timeMillis = timeMillis,
						)
					}
				}
			}
		}

		return null
	}
}

internal fun CloserScope.openTouchInputDevice(path: String): TouchInput {
	val deviceFd = open(path, O_RDONLY or O_NONBLOCK)
		.checkReturn { "Couldn't open $path" }
		.scopedUseWithClose("Closing touch input device", ::close)

	memScoped {
		val nameBufferSize = 256
		val name = allocArray<char16_tVar>(nameBufferSize)
		ioctl(deviceFd, eviocgname(nameBufferSize).toULong(), name)
		println(
			"""
			|Opened touch input device (fd: $deviceFd):
			| - device: $path
			| - name: ${name.toKString()}
			""".trimMargin(),
		)
	}

	return TouchInput(deviceFd)
}
