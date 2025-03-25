package bread_experts_group.protocol.ppp.ip

import bread_experts_group.protocol.ip.v6.InternetProtocolV6Frame
import bread_experts_group.protocol.ppp.PointToPointProtocolFrame
import java.io.InputStream
import java.io.OutputStream

class IPv6FrameEncapsulated(val frame: InternetProtocolV6Frame<*>) : PointToPointProtocolFrame(PPPProtocol.INTERNET_PROTOCOL_V6) {
	override fun calculateLength(): Int = super.calculateLength() + frame.calculateLength()

	override fun write(stream: OutputStream) {
		super.write(stream)
		frame.write(stream)
	}

	override fun protocolGist(): String = frame.gist()

	companion object {
		fun read(stream: InputStream): IPv6FrameEncapsulated {
			return IPv6FrameEncapsulated(InternetProtocolV6Frame.read(stream))
		}
	}
}