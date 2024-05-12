import kotlinx.cinterop.MemScope
import kotlinx.cinterop.alloc
import lightswitch.input_event

fun MemScope.inputEvent(type: Int, code: Int, value: Int, timeMillis: Long): input_event {
	return alloc<input_event>().also { event ->
		event.type = type.toUShort()
		event.code = code.toUShort()
		event.value = value
		event.time.tv_sec = timeMillis / 1000
		event.time.tv_usec = (timeMillis - event.time.tv_sec * 1000) * 1000
	}
}
