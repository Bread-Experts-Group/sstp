package bread_experts_group.protocol.ppp.pap

import java.io.InputStream
import java.io.OutputStream

class PAPAcknowledge(
	identifier: Int,
	val message: String,
	ok: Boolean
) : PasswordAuthenticationProtocolFrame(
	identifier,
	if (ok) PAPControlType.CONFIGURE_ACK else PAPControlType.CONFIGURE_NAK
) {
	override fun calculateLength(): Int = super.calculateLength() + 1 + message.length
	override fun write(stream: OutputStream) {
		super.write(stream)
		stream.write(message.length)
		stream.write(message.encodeToByteArray())
	}

	companion object {
		fun read(stream: InputStream, id: Int, wasOK: Boolean): PAPAcknowledge = PAPAcknowledge(
			id,
			stream.read().let { stream.readNBytes(it).decodeToString() },
			wasOK
		)
	}
}