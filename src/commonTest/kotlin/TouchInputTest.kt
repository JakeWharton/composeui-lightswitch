
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventType.Companion.Move
import androidx.compose.ui.input.pointer.PointerEventType.Companion.Press
import androidx.compose.ui.input.pointer.PointerEventType.Companion.Release
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import kotlin.test.Test
import kotlinx.cinterop.MemScope
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import lightswitch.input_event

class TouchInputTest {
	private val touch = TouchInput(0)

	@Test fun downUp() = memScoped {
		// DOWN
		assertThat(touch.process(inputEvent(3, 57, 0, 1234))).isNull()
		assertThat(touch.process(inputEvent(3, 48, 9, 1234))).isNull()
		assertThat(touch.process(inputEvent(3, 53, 52, 1234))).isNull()
		assertThat(touch.process(inputEvent(3, 54, 87, 1234))).isNull()
		assertThat(touch.process(inputEvent(0, 2, 0, 1234))).isNull()
		assertThat(touch.process(inputEvent(1, 330, 1, 1234))).isNull()
		assertThat(touch.process(inputEvent(0, 0, 0, 1234)))
			.isEqualTo(TouchEvent(Press, Offset(52f, 87f), 1234))

		// UP
		assertThat(touch.process(inputEvent(1, 330, 0, 1235))).isNull()
		assertThat(touch.process(inputEvent(0, 2, 0, 1235))).isNull()
		assertThat(touch.process(inputEvent(0, 0, 0, 1235)))
			.isEqualTo(TouchEvent(Release, Offset(52f, 87f), 1235))
	}

	@Test fun downMoveUp() = memScoped {
		// DOWN
		assertThat(touch.process(inputEvent(3, 57, 0, 1234))).isNull()
		assertThat(touch.process(inputEvent(3, 48, 9, 1234))).isNull()
		assertThat(touch.process(inputEvent(3, 53, 52, 1234))).isNull()
		assertThat(touch.process(inputEvent(3, 54, 87, 1234))).isNull()
		assertThat(touch.process(inputEvent(0, 2, 0, 1234))).isNull()
		assertThat(touch.process(inputEvent(1, 330, 1, 1234))).isNull()
		assertThat(touch.process(inputEvent(0, 0, 0, 1234)))
			.isEqualTo(TouchEvent(Press, Offset(52f, 87f), 1234))

		// MOVE
		assertThat(touch.process(inputEvent(3, 57, 0, 1234))).isNull()
		assertThat(touch.process(inputEvent(3, 48, 9, 1234))).isNull()
		assertThat(touch.process(inputEvent(3, 53, 66, 1234))).isNull()
		assertThat(touch.process(inputEvent(3, 54, 236, 1234))).isNull()
		assertThat(touch.process(inputEvent(0, 2, 0, 1234))).isNull()
		assertThat(touch.process(inputEvent(0, 0, 0, 1234)))
			.isEqualTo(TouchEvent(Move, Offset(66f, 236f), 1235))

		// UP
		assertThat(touch.process(inputEvent(1, 330, 0, 1236))).isNull()
		assertThat(touch.process(inputEvent(0, 2, 0, 1236))).isNull()
		assertThat(touch.process(inputEvent(0, 0, 0, 1236)))
			.isEqualTo(TouchEvent(Release, Offset(66f, 236f), 1236))
	}

	private fun MemScope.inputEvent(type: Int, code: Int, value: Int, timeMillis: Long): input_event {
		return alloc<input_event>().also { event ->
			event.type = type.toUShort()
			event.code = code.toUShort()
			event.value = value
			event.time.tv_sec = timeMillis / 1000
			event.time.tv_usec = (timeMillis - event.time.tv_sec * 1000) * 1000
		}
	}
}
