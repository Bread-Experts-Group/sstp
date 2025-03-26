package bread_experts_group.protocol.ppp.ip

import bread_experts_group.protocol.ip.v4.InternetProtocolFrame
import bread_experts_group.protocol.ppp.PointToPointProtocolFrame
import java.io.InputStream
import java.io.OutputStream

class IPFrameEncapsulated(val frame: InternetProtocolFrame<*>) : PointToPointProtocolFrame(PPPProtocol.INTERNET_PROTOCOL_V4) {
	override fun calculateLength(): Int = super.calculateLength() + frame.calculateLength()

	override fun write(stream: OutputStream) {
		super.write(stream)
		frame.write(stream)
	}

	override fun protocolGist(): String = frame.gist()

	companion object {
		fun read(stream: InputStream): IPFrameEncapsulated = IPFrameEncapsulated(InternetProtocolFrame.read(stream))
		fun readVJ(stream: InputStream): IPFrameEncapsulated = IPFrameEncapsulated(InternetProtocolFrame.readVJ(stream))
	}
}