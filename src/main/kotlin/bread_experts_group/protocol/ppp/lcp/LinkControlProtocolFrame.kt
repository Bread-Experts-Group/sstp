package bread_experts_group.protocol.ppp.lcp

import bread_experts_group.protocol.ppp.PPPFrame
import bread_experts_group.util.read16
import bread_experts_group.util.write16
import java.io.InputStream
import java.io.OutputStream

sealed class LinkControlProtocolFrame(
	val identifier: Int,
	val type: LinkControlType
) : PPPFrame(PPPProtocol.LINK_CONTROL_PROTOCOL) {
	override fun calculateLength(): Int = 4

	override fun write(stream: OutputStream) {
		super.write(stream)
		stream.write(this.type.code)
		stream.write(this.identifier)
		stream.write16(this.calculateLength())
	}

	companion object {
		fun read(stream: InputStream): LinkControlProtocolFrame {
			val code = LinkControlType.mapping.getValue(stream.read())
			val id = stream.read()
			val length = stream.read16() - 4
			return when (code) {
				LinkControlType.CONFIGURE_REQUEST -> LCPRequest.read(stream, id, length)
				LinkControlType.CONFIGURE_ACK -> LCPAcknowledgement.read(stream, id, length)
				LinkControlType.ECHO_REQUEST, LinkControlType.ECHO_REPLY -> LCPEcho.read(stream, id, length)
				LinkControlType.TERMINATE_REQUEST -> LCPTerminationRequest.read(stream, id, length)
				else -> TODO(code.toString())
			}
		}
	}
}