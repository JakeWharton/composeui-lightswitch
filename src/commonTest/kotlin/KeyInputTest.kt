import androidx.compose.ui.input.key.Key.Companion.Button1
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType.Companion.KeyDown
import androidx.compose.ui.input.key.KeyEventType.Companion.KeyUp
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import kotlin.test.Test
import kotlinx.cinterop.memScoped

class KeyInputTest {
	private val keys = KeyInput(0)

	@Test fun downUp() = memScoped {
		assertThat(keys.process(inputEvent(1, 60, 1, 1234)))
			.isEqualTo(KeyEvent(Button1, KeyDown))
		assertThat(keys.process(inputEvent(1, 60, 0, 1235)))
			.isEqualTo(KeyEvent(Button1, KeyUp))
	}

	@Test fun downRepeatUp() = memScoped {
		assertThat(keys.process(inputEvent(1, 60, 1, 1234)))
			.isEqualTo(KeyEvent(Button1, KeyDown))
		assertThat(keys.process(inputEvent(1, 60, 2, 1235))).isNull()
		assertThat(keys.process(inputEvent(1, 60, 0, 1236)))
			.isEqualTo(KeyEvent(Button1, KeyUp))
	}
}
