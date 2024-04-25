internal inline fun Int.checkReturn(message: () -> String) = apply {
	check(this >= 0) { message() + " ($this)" }
}

internal fun <T : Any> T?.checkNotNull(message: () -> String) = checkNotNull(this, message)

internal fun <T> T.checkEquals(expected: T, message: () -> String) {
	check(this == expected, message)
}
