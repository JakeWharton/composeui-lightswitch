package ui

import androidx.compose.runtime.State

/**
 * An abstraction of the underlying hardware so that we can run the app
 * on Desktop with a fake hardware backend.
 */
interface SwitchHardware {
	val relay: State<Boolean>
	fun onChange(value: Boolean)
}
