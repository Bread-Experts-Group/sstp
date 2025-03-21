package bread_experts_group.protocol.ppp.lcp

import java.io.InputStream
import java.io.OutputStream

class LCPTerminationRequest(
	identifier: Int,
	val data: ByteArray
) : LinkControlProtocolFrame(identifier, LinkControlType.TERMINATE_REQUEST) {
	override fun calculateLength(): Int = super.calculateLength() + data.size

	override fun write(stream: OutputStream) {
		super.write(stream)
		stream.write(data)
		stream.flush()
	}

	companion object {
		fun read(stream: InputStream, id: Int, length: Int): LCPTerminationRequest = LCPTerminationRequest(
			id, stream.readNBytes(length)
		)
	}
}