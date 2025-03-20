package bread_experts_group.protocol.ppp.ip

import bread_experts_group.protocol.ipv4.IPFrame
import bread_experts_group.protocol.ppp.PPPFrame
import java.io.InputStream
import java.io.OutputStream

class IPFrameEncapsulated(
	broadcastAddress: Int,
	unnumberedData: Int,
	val frame: IPFrame
) : PPPFrame(broadcastAddress, unnumberedData, PPPProtocol.INTERNET_PROTOCOL_V4) {
	override fun calculateLength(): Int = super.calculateLength() + frame.calculateLength()

	override fun write(stream: OutputStream) {
		super.write(stream)
		frame.write(stream)
	}

	companion object {
		fun read(stream: InputStream, broadcastAddress: Int, unnumberedData: Int): IPFrameEncapsulated {
			return IPFrameEncapsulated(broadcastAddress, unnumberedData, IPFrame.read(stream))
		}
	}
}