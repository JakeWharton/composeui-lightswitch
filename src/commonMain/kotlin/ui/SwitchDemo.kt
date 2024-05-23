package ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.platform.SystemFont
import androidx.compose.ui.text.style.TextAlign

private const val PAGE_COUNT_REAL = 5
private const val PAGE_COUNT_FAKE = Short.MAX_VALUE.toInt()
private const val PAGE_INITIAL = PAGE_COUNT_FAKE / 2
private const val PAGE_INDEX_SWITCH = (PAGE_INITIAL + 0) % PAGE_COUNT_REAL
private const val PAGE_INDEX_DASHBOARD = (PAGE_INITIAL + 1) % PAGE_COUNT_REAL
private const val PAGE_INDEX_HOME_ASSISTANT = (PAGE_INITIAL + 2) % PAGE_COUNT_REAL
private const val PAGE_INDEX_MUSIC = (PAGE_INITIAL + 3) % PAGE_COUNT_REAL
private const val PAGE_INDEX_SETTINGS = (PAGE_INITIAL + 4) % PAGE_COUNT_REAL

@Composable
fun SwitchDemo(
	hardware: SwitchHardware,
// 	modifier: Modifier = Modifier,
) {
	PageSwitch(hardware)

	// TODO This deadlocks on the device when you page.
// 	val pagerState = rememberPagerState(initialPage = PAGE_INITIAL) { PAGE_COUNT_FAKE }
// 	HorizontalPager(
// 		state = pagerState,
// 		beyondBoundsPageCount = 1,
// 		modifier = modifier,
// 	) { page ->
// 		when (page % PAGE_COUNT_REAL) {
// 			PAGE_INDEX_DASHBOARD -> PageDashboard()
// 			PAGE_INDEX_SWITCH -> PageSwitch(hardware)
// 			PAGE_INDEX_HOME_ASSISTANT -> PageHomeAssistant()
// 			PAGE_INDEX_MUSIC -> PageMusic()
// 			PAGE_INDEX_SETTINGS -> PageSettings()
// 			else -> throw AssertionError()
// 		}
// 	}
}

@Composable
private fun PageDashboard() {
	Text(
		text = "Dashboard",
		color = MaterialTheme.colorScheme.onSurface,
		textAlign = TextAlign.Center,
		modifier = Modifier.fillMaxWidth(),
		fontFamily = FontFamily(SystemFont("NanumGothic")),
	)
}

@Composable
private fun PageSwitch(hardware: SwitchHardware) {
	Box(
		contentAlignment = Alignment.Center,
		modifier = Modifier.fillMaxSize(),
	) {
		ToggleSwitch(
			checked = hardware.relay.value,
			onCheckedChange = hardware::onChange,
		)
	}
}

@Composable
private fun PageHomeAssistant() {
	Text(
		text = "Home Assistant",
		color = MaterialTheme.colorScheme.onSurface,
		textAlign = TextAlign.Center,
		modifier = Modifier.fillMaxWidth(),
		fontFamily = FontFamily(SystemFont("NanumGothic")),
	)
}

@Composable
private fun PageMusic() {
	Text(
		text = "Music",
		color = MaterialTheme.colorScheme.onSurface,
		textAlign = TextAlign.Center,
		modifier = Modifier.fillMaxWidth(),
		fontFamily = FontFamily(SystemFont("NanumGothic")),
	)
}

@Composable
private fun PageSettings() {
	Text(
		text = "Settings",
		color = MaterialTheme.colorScheme.onSurface,
		textAlign = TextAlign.Center,
		modifier = Modifier.fillMaxWidth(),
		fontFamily = FontFamily(SystemFont("NanumGothic")),
	)
}
