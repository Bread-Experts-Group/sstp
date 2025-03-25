package bread_experts_group.protocol.ppp.lcp

import bread_experts_group.util.read16
import bread_experts_group.util.write16
import java.io.InputStream
import java.io.OutputStream

class LCPProtocolRejection(
	identifier: Int,
	val rejectedProtocol: PPPProtocol,
	val data: ByteArray
) : LinkControlProtocolFrame(identifier, LCPControlType.PROTOCOL_REJECT) {
	override fun calculateLength(): Int = super.calculateLength() + 4 + data.size
	override fun write(stream: OutputStream) {
		super.write(stream)
		stream.write16(rejectedProtocol.code)
		stream.write(data)
		stream.flush()
	}

	override fun lcpGist(): String = "PROTOCOL: ${rejectedProtocol.name}, # DATA: [${data.size}]"

	companion object {
		fun read(stream: InputStream, identifier: Int, length: Int): LCPProtocolRejection = LCPProtocolRejection(
			identifier,
			PPPProtocol.mapping.getValue(stream.read16()),
			stream.readNBytes(length - 4)
		)
	}
}