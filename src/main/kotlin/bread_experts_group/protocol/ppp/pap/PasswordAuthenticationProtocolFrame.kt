package bread_experts_group.protocol.ppp.pap

import bread_experts_group.protocol.ppp.PPPFrame
import bread_experts_group.util.read16
import bread_experts_group.util.write16
import java.io.InputStream
import java.io.OutputStream

sealed class PasswordAuthenticationProtocolFrame(
	broadcastAddress: Int,
	unnumberedData: Int,
	val identifier: Int,
	val type: PAPControlType
) : PPPFrame(broadcastAddress, unnumberedData, PPPProtocol.PASSWORD_AUTHENTICATION_PROTOCOL) {
	override fun calculateLength(): Int = 4

	override fun write(stream: OutputStream) {
		super.write(stream)
		stream.write(this.type.code)
		stream.write(this.identifier)
		stream.write16(this.calculateLength())
	}

	companion object {
		fun read(stream: InputStream, broadcastAddress: Int, unnumberedData: Int): PasswordAuthenticationProtocolFrame {
			val code = PAPControlType.mapping.getValue(stream.read())
			val id = stream.read()
			val length = stream.read16() - 4
			return when (code) {
				PAPControlType.CONFIGURE_REQUEST ->
					PasswordAuthenticationRequest.read(stream, broadcastAddress, unnumberedData, id)

				PAPControlType.CONFIGURE_ACK ->
					PasswordAuthenticationAcknowledge.read(stream, broadcastAddress, unnumberedData, id, true)

				PAPControlType.CONFIGURE_NAK ->
					PasswordAuthenticationAcknowledge.read(stream, broadcastAddress, unnumberedData, id, false)
			}
		}
	}
}