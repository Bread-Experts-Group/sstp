package bread_experts_group.protocol.ppp.pap

import java.io.InputStream
import java.io.OutputStream

class PAPRequest(
	identifier: Int,
	val peerID: String,
	val password: String
) : PasswordAuthenticationProtocolFrame(identifier, PAPControlType.CONFIGURE_REQUEST) {
	override fun calculateLength(): Int = super.calculateLength() + 2 + peerID.length + password.length
	override fun write(stream: OutputStream) {
		super.write(stream)
		stream.write(peerID.length)
		stream.write(peerID.encodeToByteArray())
		stream.write(password.length)
		stream.write(password.encodeToByteArray())
	}

	companion object {
		fun read(stream: InputStream, id: Int): PAPRequest = PAPRequest(
			id,
			stream.read().let { stream.readNBytes(it).decodeToString() },
			stream.read().let { stream.readNBytes(it).decodeToString() }
		)
	}
}