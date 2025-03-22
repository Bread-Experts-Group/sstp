package bread_experts_group.protocol.ppp.lcp

import java.io.InputStream
import java.io.OutputStream

class LCPTermination(
	identifier: Int,
	val data: ByteArray,
	request: Boolean
) : LinkControlProtocolFrame(identifier, if (request) LCPControlType.TERMINATE_REQUEST else LCPControlType.TERMINATE_ACK) {
	override fun calculateLength(): Int = super.calculateLength() + data.size

	override fun write(stream: OutputStream) {
		super.write(stream)
		stream.write(data)
		stream.flush()
	}

	override fun lcpGist(): String = "DATA: \"${data.decodeToString()}\""

	companion object {
		fun read(stream: InputStream, id: Int, length: Int, request: Boolean): LCPTermination = LCPTermination(
			id, stream.readNBytes(length), request
		)
	}
}