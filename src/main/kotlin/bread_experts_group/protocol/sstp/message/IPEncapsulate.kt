package bread_experts_group.protocol.sstp.message

import bread_experts_group.protocol.ipv4.IPFrame
import bread_experts_group.protocol.ppp.ip.InternetProtocolFrameEncapsulated
import java.io.OutputStream

class IPEncapsulate<T : IPFrame>(ipFrame: T) : PPPEncapsulate<InternetProtocolFrameEncapsulated>(
	InternetProtocolFrameEncapsulated(ipFrame)
) {
	override fun write(stream: OutputStream) {
		super.write(stream)
		stream.flush()
	}
}