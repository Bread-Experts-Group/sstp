package bread_experts_group.protocol.ppp.lcp

import bread_experts_group.util.hex
import bread_experts_group.util.read32
import java.io.InputStream
import java.io.OutputStream

class LCPProtocolRejection(
	identifier: Int,
	val magic: Int,
	val data: ByteArray
) : LinkControlProtocolFrame(identifier, LCPControlType.DISCARD_REQUEST) {
	override fun calculateLength(): Int = super.calculateLength() + 4 + data.size
	override fun write(stream: OutputStream) = TODO("Protocol rejections")

	override fun lcpGist(): String = "MAGIC: ${hex(magic)}, # DATA: [${data.size}]"

	companion object {
		fun read(stream: InputStream, identifier: Int, length: Int): LCPProtocolRejection = LCPProtocolRejection(
			identifier,
			stream.read32(),
			stream.readNBytes(length - 4)
		)
	}
}