package ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.platform.SystemFont
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.coerceIn
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ToggleSwitch(
	checked: Boolean,
	onCheckedChange: ((Boolean) -> Unit)?,
	modifier: Modifier = Modifier,
) {
	// Use a State wrapper so that the input closures don't capture the
	// original `checked` value and never change it.
	var checkedAsState by remember { mutableStateOf(checked) }
	checkedAsState = checked

	val density = LocalDensity.current

	var dragOffset: Dp? by remember { mutableStateOf(null) }
	val borderPadding = 5.dp
	val toggleHeight = 100.dp
	val totalHeight = 240.dp
	val totalWidth = 80.dp

	val maxOffset = totalHeight - toggleHeight - (borderPadding * 2)

	val resting = if (checked) 0.dp else maxOffset
	val togglePositionValue = (resting + (dragOffset ?: 0.dp))
		.coerceIn(0.dp, maxOffset)
	val togglePosition by animateDpAsState(
		togglePositionValue,
		label = "Switch toggle position",
	)

	// The value of "checked" if we were to commit the currently active drag operation
	var checkedIfCommitted by remember { mutableStateOf(true) }
	checkedIfCommitted = togglePosition < maxOffset / 2

	val toggleColor by animateColorAsState(
		if (checkedIfCommitted) Color.Yellow else Color.DarkGray,
		label = "Switch toggle color",
	)

	val labelColor by animateColorAsState(
		if (checkedIfCommitted) Color.DarkGray else Color.LightGray,
		label = "Switch toggle label color",
	)

	Box(
		modifier = modifier
			.size(width = totalWidth, height = totalHeight)
			.background(Color.Gray, RoundedCornerShape(10.dp))
			.padding(borderPadding)
			.pointerInput(Unit) {
				detectVerticalDragGestures(
					onDragStart = { _ ->
						dragOffset = 0.dp
					},
					onVerticalDrag = { change, dragAmount ->
						dragOffset = dragOffset!! + with(density) { dragAmount.toDp() }
						change.consume()
					},
					onDragEnd = {
						dragOffset = null
						if (checkedIfCommitted != checkedAsState) {
							onCheckedChange?.invoke(checkedIfCommitted)
						}
					},
					onDragCancel = {
						dragOffset = null
					},
				)
			}
			.pointerInput(Unit) {
				detectTapGestures(
					onTap = {
						val updatedChecked = !checkedAsState
						onCheckedChange?.invoke(updatedChecked)
					},
				)
			},
		contentAlignment = Alignment.TopCenter,
	) {
		// The toggle switch
		Box(
			modifier = Modifier
				.offset(y = togglePosition)
				.size(totalWidth - borderPadding * 2, toggleHeight)
				.background(toggleColor, RoundedCornerShape(10.dp)),
		) {
			Text(
				text = if (checkedIfCommitted) "ON" else "OFF",
				modifier = Modifier.align(Alignment.Center),
				color = labelColor,
				fontSize = 20.sp,
				fontFamily = FontFamily(SystemFont("NanumGothic")),
			)
		}
	}
}
