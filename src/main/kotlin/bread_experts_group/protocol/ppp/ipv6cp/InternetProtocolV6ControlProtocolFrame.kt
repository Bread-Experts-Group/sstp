package bread_experts_group.protocol.ppp.ipv6cp

import bread_experts_group.protocol.ppp.ControlType
import bread_experts_group.protocol.ppp.PPPFrame
import bread_experts_group.util.read16
import bread_experts_group.util.write16
import java.io.InputStream
import java.io.OutputStream

sealed class InternetProtocolV6ControlProtocolFrame(
	val identifier: Int,
	val type: ControlType
) : PPPFrame(PPPProtocol.INTERNET_PROTOCOL_V6_CONTROL_PROTOCOL) {
	override fun calculateLength(): Int = 4

	override fun write(stream: OutputStream) {
		super.write(stream)
		stream.write(this.type.code)
		stream.write(this.identifier)
		stream.write16(this.calculateLength())
	}

	companion object {
		fun read(stream: InputStream): InternetProtocolV6ControlProtocolFrame {
			val code = ControlType.Companion.mapping.getValue(stream.read())
			val id = stream.read()
			val length = stream.read16() - 4
			return when (code) {
				ControlType.CONFIGURE_REQUEST -> IPv6CPRequest.read(stream, id, length)
				else -> TODO(code.toString())
			}
		}
	}
}