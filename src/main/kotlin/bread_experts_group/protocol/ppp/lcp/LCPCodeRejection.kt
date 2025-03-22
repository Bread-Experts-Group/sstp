package bread_experts_group.protocol.ppp.lcp

import java.io.InputStream
import java.io.OutputStream

class LCPCodeRejection(
	identifier: Int,
	val rejectedPacket: ByteArray
) : LinkControlProtocolFrame(identifier, LCPControlType.CODE_REJECT) {
	override fun calculateLength(): Int = super.calculateLength() + 2 + rejectedPacket.size
	override fun write(stream: OutputStream) = TODO("Protocol rejections")

	override fun lcpGist(): String = "# DATA: [${rejectedPacket.size}]"

	companion object {
		fun read(stream: InputStream, identifier: Int, length: Int): LCPCodeRejection = LCPCodeRejection(
			identifier,
			stream.readNBytes(length)
		)
	}
}