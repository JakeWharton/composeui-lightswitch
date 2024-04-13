
import kotlin.contracts.InvocationKind.EXACTLY_ONCE
import kotlin.contracts.contract
import kotlinx.cinterop.CStructVar
import kotlinx.cinterop.CValue
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.placeTo
import kotlinx.cinterop.pointed

// https://youtrack.jetbrains.com/issue/KT-66169
inline fun <reified T : CStructVar, R> CValue<T>.useContents(block: T.() -> R): R {
	contract {
		callsInPlace(block, EXACTLY_ONCE)
	}
	return memScoped {
		placeTo(this).pointed.block()
	}
}
