package bread_experts_group.protocol.ppp.ipcp

import bread_experts_group.protocol.ppp.NCPControlType
import java.io.InputStream
import java.io.OutputStream

class IPCPTerminationRequest(
	identifier: Int,
	val data: ByteArray
) : InternetProtocolControlProtocolFrame(identifier, NCPControlType.TERMINATE_REQUEST) {
	override fun calculateLength(): Int = super.calculateLength() + data.size

	override fun write(stream: OutputStream) {
		super.write(stream)
		stream.write(data)
		stream.flush()
	}

	companion object {
		fun read(stream: InputStream, id: Int, length: Int): IPCPTerminationRequest = IPCPTerminationRequest(
			id, stream.readNBytes(length)
		)
	}
}