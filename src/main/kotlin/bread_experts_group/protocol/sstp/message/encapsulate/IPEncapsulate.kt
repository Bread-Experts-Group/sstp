package bread_experts_group.protocol.sstp.message.encapsulate

import bread_experts_group.protocol.ip.InternetProtocol
import bread_experts_group.protocol.ip.v4.InternetProtocolFrame
import bread_experts_group.protocol.ppp.ip.IPFrameEncapsulated
import java.io.OutputStream

class IPEncapsulate<T : InternetProtocolFrame<R>, R : InternetProtocol>(ipFrame: T) : PPPEncapsulate<IPFrameEncapsulated>(
	IPFrameEncapsulated(ipFrame)
) {
	override fun write(stream: OutputStream) {
		super.write(stream)
//		if (PointToPointProtocolFrame.compression.outbound.vjIP && this.pppFrame.frame.data is TCPFrame) {
//
//		}
	}
}