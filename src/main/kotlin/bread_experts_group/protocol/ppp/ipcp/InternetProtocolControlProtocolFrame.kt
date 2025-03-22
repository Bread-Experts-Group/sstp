package bread_experts_group.protocol.ppp.ipcp

import bread_experts_group.protocol.ppp.NCPControlType
import bread_experts_group.protocol.ppp.PointToPointProtocolFrame
import bread_experts_group.util.read16
import bread_experts_group.util.write16
import java.io.InputStream
import java.io.OutputStream

sealed class InternetProtocolControlProtocolFrame(
	val identifier: Int,
	val type: NCPControlType
) : PointToPointProtocolFrame(PPPProtocol.INTERNET_PROTOCOL_CONTROL_PROTOCOL) {
	override fun calculateLength(): Int = 4

	override fun write(stream: OutputStream) {
		super.write(stream)
		stream.write(this.type.code)
		stream.write(this.identifier)
		stream.write16(this.calculateLength())
	}

	companion object {
		fun read(stream: InputStream): InternetProtocolControlProtocolFrame {
			val code = NCPControlType.Companion.mapping.getValue(stream.read())
			val id = stream.read()
			val length = stream.read16() - 4
			return when (code) {
				NCPControlType.CONFIGURE_REQUEST -> IPCPRequest.read(stream, id, length)
				NCPControlType.CONFIGURE_ACK -> IPCPAcknowledgement.read(stream, id, length)
				NCPControlType.TERMINATE_REQUEST -> IPCPTerminationRequest.read(stream, id, length)
				else -> TODO(code.toString())
			}
		}
	}
}