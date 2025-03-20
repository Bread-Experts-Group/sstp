package bread_experts_group.protocol.ppp.ipcp

import bread_experts_group.protocol.ppp.ControlType
import bread_experts_group.protocol.ppp.PPPFrame
import bread_experts_group.util.read16
import bread_experts_group.util.write16
import java.io.InputStream
import java.io.OutputStream

sealed class InternetProtocolControlProtocolFrame(
	broadcastAddress: Int,
	unnumberedData: Int,
	val identifier: Int,
	val type: ControlType
) : PPPFrame(broadcastAddress, unnumberedData, PPPProtocol.INTERNET_PROTOCOL_CONTROL_PROTOCOL) {
	override fun calculateLength(): Int = 4

	override fun write(stream: OutputStream) {
		super.write(stream)
		stream.write(this.type.code)
		stream.write(this.identifier)
		stream.write16(this.calculateLength())
	}

	companion object {
		fun read(stream: InputStream, broadcastAddress: Int, unnumberedData: Int): InternetProtocolControlProtocolFrame {
			val code = ControlType.Companion.mapping.getValue(stream.read())
			val id = stream.read()
			val length = stream.read16() - 4
			return when (code) {
				ControlType.CONFIGURE_REQUEST ->
					InternetProtocolControlConfigurationRequest.read(stream, broadcastAddress, unnumberedData, id, length)

				ControlType.CONFIGURE_ACK ->
					InternetProtocolControlConfigurationAcknowledgement.read(stream, broadcastAddress, unnumberedData, id, length)

				ControlType.TERMINATE_REQUEST ->
					InternetProtocolControlTerminationRequest.read(stream, broadcastAddress, unnumberedData, id, length)

				else -> TODO(code.toString())
			}
		}
	}
}