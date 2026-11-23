import io.ktor.network.selector.SelectorManager
import io.ktor.network.sockets.UnixSocketAddress
import io.ktor.network.sockets.aSocket
import io.ktor.network.sockets.openReadChannel
import io.ktor.network.sockets.openWriteChannel
import io.ktor.utils.io.core.readUTF8Line
import io.ktor.utils.io.writeStringUtf8
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

internal class EmberHost private constructor(
	private val uds: UnixSocketAddress,
	private val nodeId: String,
) {
	suspend fun setStatus(value: Boolean) {
		val command = if (value) "on" else "off"
		uds.readWrite("""e SerialbusEngine TestModeCommand {"test_485_ctrl":"$command","node_id":"$nodeId"}""")
	}

	suspend fun getStatus(): Boolean {
		val json = uds.readWrite("""e SerialbusEngine TestModeCommand {"test_485_ctrl":"readOnOffStatus","node_id":"$nodeId"}""")
		val response = Json.decodeFromString(StatusResponse.serializer(), json)
		// TODO Check status == 0?
		return response.value
	}

	@Serializable
	private data class StatusResponse(
		val status: Int,
		val value: Boolean,
	)

	@Serializable
	private data class DeviceListResponse(
		val device_type: String,
		val endpoints_num: Int,
		val node_id: List<String>,
	)

	companion object {
		private val tcp = aSocket(SelectorManager(Dispatchers.Default)).tcp()

		private suspend fun UnixSocketAddress.readWrite(value: String): String {
			println("UDS --> $value")

			// For some reason, we can't read/write multiple lines and instead have to put each
			// request/response pair on a dedicated "connection" to the UDS.
			val socket = tcp.connect(this)

			val writer = socket.openWriteChannel()
			writer.writeStringUtf8(value)
			writer.close(null)

			val reader = socket.openReadChannel()
			return reader.readRemaining().readUTF8Line().orEmpty().also {
				println("UDS <-- $it")
			}
		}

		suspend fun initialize(path: String): EmberHost {
			val uds = UnixSocketAddress(path)

			val json = uds.readWrite("""e SerialbusEngine TestModeCommand {"test_485_ctrl":"devicelist"}""")
			val responses = Json.decodeFromString(ListSerializer(DeviceListResponse.serializer()), json)
			val response = responses.singleOrNull() ?: throw IllegalStateException(json)
			require(response.device_type == "DT_DIMMER_FOR_D") { json }
			val nodeId = response.node_id.singleOrNull() ?: throw IllegalStateException(json)

			return EmberHost(uds, nodeId)
		}
	}
}
