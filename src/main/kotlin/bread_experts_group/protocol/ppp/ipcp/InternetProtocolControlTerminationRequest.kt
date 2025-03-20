package bread_experts_group.protocol.ppp.ipcp

import bread_experts_group.protocol.ppp.ControlType
import java.io.InputStream
import java.io.OutputStream

class InternetProtocolControlTerminationRequest(
	broadcastAddress: Int,
	unnumberedData: Int,
	identifier: Int,
	val data: ByteArray
) : InternetProtocolControlProtocolFrame(broadcastAddress, unnumberedData, identifier, ControlType.TERMINATE_REQUEST) {
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
		): InternetProtocolControlTerminationRequest = InternetProtocolControlTerminationRequest(
			broadcastAddress, unnumberedData, id,
			stream.readNBytes(length)
		)
	}
}