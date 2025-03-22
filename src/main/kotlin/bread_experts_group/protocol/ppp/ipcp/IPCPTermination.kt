package bread_experts_group.protocol.ppp.ipcp

import bread_experts_group.protocol.ppp.NCPControlType
import java.io.InputStream
import java.io.OutputStream

class IPCPTermination(
	identifier: Int,
	val data: ByteArray,
	request: Boolean
) : InternetProtocolControlProtocolFrame(
	identifier,
	if (request) NCPControlType.TERMINATE_REQUEST else NCPControlType.TERMINATE_ACK
) {
	override fun calculateLength(): Int = super.calculateLength() + data.size

	override fun write(stream: OutputStream) {
		super.write(stream)
		stream.write(data)
		stream.flush()
	}

	override fun ipcpGist(): String = "DATA: \"${data.decodeToString()}\""

	companion object {
		fun read(stream: InputStream, id: Int, length: Int, request: Boolean): IPCPTermination = IPCPTermination(
			id, stream.readNBytes(length), request
		)
	}
}