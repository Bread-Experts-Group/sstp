package bread_experts_group.protocol.ppp.pap

import bread_experts_group.protocol.ppp.PointToPointProtocolFrame
import bread_experts_group.util.read16
import bread_experts_group.util.write16
import java.io.InputStream
import java.io.OutputStream

sealed class PasswordAuthenticationProtocolFrame(
	val identifier: Int,
	val type: PAPControlType
) : PointToPointProtocolFrame(PPPProtocol.PASSWORD_AUTHENTICATION_PROTOCOL) {
	override fun calculateLength(): Int = 4

	override fun write(stream: OutputStream) {
		super.write(stream)
		stream.write(this.type.code)
		stream.write(this.identifier)
		stream.write16(this.calculateLength())
	}

	companion object {
		fun read(stream: InputStream): PasswordAuthenticationProtocolFrame {
			val code = PAPControlType.mapping.getValue(stream.read())
			val id = stream.read()
			stream.read16() - 4 // length
			return when (code) {
				PAPControlType.CONFIGURE_REQUEST -> PAPRequest.read(stream, id)
				PAPControlType.CONFIGURE_ACK -> PAPAcknowledge.read(stream, id, true)
				PAPControlType.CONFIGURE_NAK -> PAPAcknowledge.read(stream, id, false)
			}
		}
	}
}