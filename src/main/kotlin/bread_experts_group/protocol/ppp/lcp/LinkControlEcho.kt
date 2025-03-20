package bread_experts_group.protocol.ppp.lcp

import bread_experts_group.util.read32
import bread_experts_group.util.write32
import java.io.InputStream
import java.io.OutputStream

class LinkControlEcho(
	broadcastAddress: Int,
	unnumberedData: Int,
	identifier: Int,
	val magic: Int,
	val data: ByteArray,
	request: Boolean
) : LinkControlProtocolFrame(
	broadcastAddress, unnumberedData, identifier,
	if (request) LinkControlType.ECHO_REQUEST else LinkControlType.ECHO_REPLY
) {
	override fun calculateLength(): Int = super.calculateLength() + 4 + data.size
	override fun write(stream: OutputStream) {
		super.write(stream)
		stream.write32(this.magic)
		stream.write(data)
	}

	companion object {
		fun read(
			stream: InputStream,
			broadcastAddress: Int, unnumberedData: Int, id: Int, length: Int
		): LinkControlEcho = LinkControlEcho(
			broadcastAddress, unnumberedData, id,
			stream.read32(), stream.readNBytes(length),
			true
		)
	}
}