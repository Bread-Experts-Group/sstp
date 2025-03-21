package bread_experts_group.protocol.ppp.lcp

import bread_experts_group.util.hex
import bread_experts_group.util.read32
import bread_experts_group.util.write32
import java.io.InputStream
import java.io.OutputStream

class LCPEcho(
	identifier: Int,
	val magic: Int,
	val data: ByteArray,
	request: Boolean
) : LinkControlProtocolFrame(
	identifier,
	if (request) LCPControlType.ECHO_REQUEST else LCPControlType.ECHO_REPLY
) {
	override fun calculateLength(): Int = super.calculateLength() + 4 + data.size
	override fun write(stream: OutputStream) {
		super.write(stream)
		stream.write32(this.magic)
		stream.write(data)
	}

	override fun lcpGist(): String = "MAGIC: ${hex(magic)}, # DATA: [${data.size}]"

	companion object {
		fun read(stream: InputStream, id: Int, length: Int): LCPEcho = LCPEcho(
			id,
			stream.read32(), stream.readNBytes(length),
			true
		)
	}
}