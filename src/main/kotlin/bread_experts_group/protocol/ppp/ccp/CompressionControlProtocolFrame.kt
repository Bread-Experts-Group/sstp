package bread_experts_group.protocol.ppp.ccp

import bread_experts_group.protocol.ppp.NCPControlType
import bread_experts_group.protocol.ppp.PointToPointProtocolFrame
import bread_experts_group.util.read16
import bread_experts_group.util.write16
import java.io.InputStream
import java.io.OutputStream

sealed class CompressionControlProtocolFrame(
	val identifier: Int,
	val type: NCPControlType
) : PointToPointProtocolFrame(PPPProtocol.COMPRESSION_CONTROL_PROTOCOL) {
	override fun calculateLength(): Int = 4

	override fun write(stream: OutputStream) {
		super.write(stream)
		stream.write(this.type.code)
		stream.write(this.identifier)
		stream.write16(this.calculateLength())
	}

	override fun protocolGist(): String = "$type, ID: $identifier\n${ccpGist()}"
	abstract fun ccpGist(): String

	companion object {
		fun read(stream: InputStream): CompressionControlProtocolFrame {
			val code = NCPControlType.Companion.mapping.getValue(stream.read())
			val id = stream.read()
			val length = stream.read16() - 4
			return when (code) {
				NCPControlType.CONFIGURE_REQUEST -> CCPRequest(stream, length, id)
				NCPControlType.CONFIGURE_NAK -> CCPNonAcknowledgement(stream, length, id)
				NCPControlType.CONFIGURE_ACK -> CCPAcknowledgement(stream, length, id)
				else -> TODO(code.toString())
			}
		}
	}
}