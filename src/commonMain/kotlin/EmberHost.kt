import io.ktor.network.selector.SelectorManager
import io.ktor.network.sockets.UnixSocketAddress
import io.ktor.network.sockets.aSocket
import io.ktor.network.sockets.openReadChannel
import io.ktor.network.sockets.openWriteChannel
import io.ktor.utils.io.core.readUTF8Line
import io.ktor.utils.io.writeStringUtf8
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

private const val NODE_ID = "7a41b0411ed9448b9b6eccd14958f197"

internal class EmberHost(
	path: String,
) {
	private val uds = UnixSocketAddress(path)
	private val tcp = aSocket(SelectorManager(Dispatchers.Default)).tcp()

	suspend fun setStatus(value: Boolean) {
		val command = if (value) "on" else "off"
		readWrite("""e SerialbusEngine TestModeCommand {"test_485_ctrl":"$command","node_id":"$NODE_ID"}""")
	}

	suspend fun getStatus(): Boolean {
		val json = readWrite("""e SerialbusEngine TestModeCommand {"test_485_ctrl":"readOnOffStatus","node_id":"$NODE_ID"}""")
		val response = Json.decodeFromString(StatusResponse.serializer(), json)
		// TODO Check status == 0?
		return response.value
	}

	private suspend fun readWrite(value: String): String {
		// For some reason, we can't read/write multiple lines and instead have to put each
		// request/response pair on a dedicated "connection" to the UDS.
		val socket = tcp.connect(uds)

		val writer = socket.openWriteChannel()
		writer.writeStringUtf8(value)
		writer.close(null)

		val reader = socket.openReadChannel()
		return reader.readRemaining().readUTF8Line().orEmpty()
	}

	@Serializable
	private data class StatusResponse(
		val status: Int,
		val value: Boolean,
	)
}
