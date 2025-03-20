package bread_experts_group.protocol.ppp.lcp

import bread_experts_group.protocol.ppp.PPPFrame
import bread_experts_group.util.read16
import bread_experts_group.util.write16
import java.io.InputStream
import java.io.OutputStream

sealed class LinkControlProtocolFrame(
	broadcastAddress: Int,
	unnumberedData: Int,
	val identifier: Int,
	val type: LinkControlType
) : PPPFrame(broadcastAddress, unnumberedData, PPPProtocol.LINK_CONTROL_PROTOCOL) {
	override fun calculateLength(): Int = 4

	override fun write(stream: OutputStream) {
		super.write(stream)
		stream.write(this.type.code)
		stream.write(this.identifier)
		stream.write16(this.calculateLength())
	}

	companion object {
		fun read(stream: InputStream, broadcastAddress: Int, unnumberedData: Int): LinkControlProtocolFrame {
			val code = LinkControlType.mapping.getValue(stream.read())
			val id = stream.read()
			val length = stream.read16() - 4
			return when (code) {
				LinkControlType.CONFIGURE_REQUEST ->
					LinkControlConfigurationRequest.read(stream, broadcastAddress, unnumberedData, id, length)

				LinkControlType.CONFIGURE_ACK ->
					LinkControlConfigurationAcknowledgement.read(stream, broadcastAddress, unnumberedData, id, length)

				LinkControlType.ECHO_REQUEST, LinkControlType.ECHO_REPLY ->
					LinkControlEcho.read(stream, broadcastAddress, unnumberedData, id, length)

				LinkControlType.TERMINATE_REQUEST ->
					LinkControlTerminationRequest.read(stream, broadcastAddress, unnumberedData, id, length)

				else -> TODO(code.toString())
			}
		}
	}
}