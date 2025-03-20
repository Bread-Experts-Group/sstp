package bread_experts_group.protocol.ppp.pap

import java.io.InputStream
import java.io.OutputStream

class PasswordAuthenticationRequest(
	broadcastAddress: Int,
	unnumberedData: Int,
	identifier: Int,
	val peerID: String,
	val password: String
) : PasswordAuthenticationProtocolFrame(broadcastAddress, unnumberedData, identifier, PAPControlType.CONFIGURE_REQUEST) {
	override fun calculateLength(): Int = super.calculateLength() + 2 + peerID.length + password.length
	override fun write(stream: OutputStream) {
		super.write(stream)
		stream.write(peerID.length)
		stream.write(peerID.encodeToByteArray())
		stream.write(password.length)
		stream.write(password.encodeToByteArray())
	}

	companion object {
		fun read(
			stream: InputStream,
			broadcastAddress: Int, unnumberedData: Int, id: Int
		): PasswordAuthenticationRequest = PasswordAuthenticationRequest(
			broadcastAddress, unnumberedData, id,
			stream.read().let { stream.readNBytes(it).decodeToString() },
			stream.read().let { stream.readNBytes(it).decodeToString() }
		)
	}
}