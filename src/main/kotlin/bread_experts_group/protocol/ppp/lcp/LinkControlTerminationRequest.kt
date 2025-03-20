package bread_experts_group.protocol.ppp.lcp

import java.io.InputStream
import java.io.OutputStream

class LinkControlTerminationRequest(
	broadcastAddress: Int,
	unnumberedData: Int,
	identifier: Int,
	val data: ByteArray
) : LinkControlProtocolFrame(broadcastAddress, unnumberedData, identifier, LinkControlType.TERMINATE_REQUEST) {
	override fun calculateLength(): Int = super.calculateLength() + data.size

	override fun write(stream: OutputStream) {
		super.write(stream)
		stream.write(data)
		stream.flush()
	}

	companion object {
		fun read(
			stream: InputStream,
			broadcastAddress: Int, unnumberedData: Int, id: Int, length: Int
		): LinkControlTerminationRequest = LinkControlTerminationRequest(
			broadcastAddress, unnumberedData, id,
			stream.readNBytes(length)
		)
	}
}