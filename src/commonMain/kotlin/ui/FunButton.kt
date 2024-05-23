package ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.platform.SystemFont
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.random.Random

@Composable
fun FunButton(
	hardware: SwitchHardware,
	modifier: Modifier = Modifier,
) {
	val initialColor = MaterialTheme.colorScheme.primary
	var color by remember { mutableStateOf(initialColor) }
	Button(
		onClick = {
			color = Color(Random.nextInt(0xFFFFFF) or 0xFF000000.toInt())
		},
		colors = ButtonDefaults.buttonColors(containerColor = color),
		modifier = modifier.padding(16.dp).fillMaxSize(),
	) {
		Text(
			if (hardware.relay.value) "ON" else "OFF",
			fontSize = 50.sp,
			fontFamily = FontFamily(SystemFont("NanumGothic")),
		)
	}
}
