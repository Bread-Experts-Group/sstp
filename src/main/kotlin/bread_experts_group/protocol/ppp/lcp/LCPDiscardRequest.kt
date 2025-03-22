package bread_experts_group.protocol.ppp.lcp

import bread_experts_group.util.read16
import java.io.InputStream
import java.io.OutputStream

class LCPDiscardRequest(
	identifier: Int,
	val rejectedProtocol: Int,
	val rejectedPacket: ByteArray
) : LinkControlProtocolFrame(identifier, LCPControlType.PROTOCOL_REJECT) {
	override fun calculateLength(): Int = super.calculateLength() + 2 + rejectedPacket.size
	override fun write(stream: OutputStream) = TODO("Protocol rejections")

	override fun lcpGist(): String = "PROTOCOL: $rejectedProtocol, # DATA: [${rejectedPacket.size}]"

	companion object {
		fun read(stream: InputStream, identifier: Int, length: Int): LCPDiscardRequest = LCPDiscardRequest(
			identifier,
			stream.read16(),
			stream.readNBytes(length - 2)
		)
	}
}