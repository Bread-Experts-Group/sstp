package bread_experts_group.protocol.ppp.pap

import java.io.InputStream
import java.io.OutputStream

class PasswordAuthenticationAcknowledge(
	broadcastAddress: Int,
	unnumberedData: Int,
	identifier: Int,
	val message: String,
	ok: Boolean
) : PasswordAuthenticationProtocolFrame(
	broadcastAddress, unnumberedData, identifier,
	if (ok) PAPControlType.CONFIGURE_ACK else PAPControlType.CONFIGURE_NAK
) {
	override fun calculateLength(): Int = super.calculateLength() + 1 + message.length
	override fun write(stream: OutputStream) {
		super.write(stream)
		stream.write(message.length)
		stream.write(message.encodeToByteArray())
	}

	companion object {
		fun read(
			stream: InputStream,
			broadcastAddress: Int, unnumberedData: Int, id: Int,
			wasOK: Boolean
		): PasswordAuthenticationAcknowledge = PasswordAuthenticationAcknowledge(
			broadcastAddress, unnumberedData, id,
			stream.read().let { stream.readNBytes(it).decodeToString() },
			wasOK
		)
	}
}