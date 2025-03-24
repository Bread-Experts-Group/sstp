package bread_experts_group.protocol.sstp.message.encapsulate

import bread_experts_group.protocol.ipv4.InternetProtocolFrame
import bread_experts_group.protocol.ppp.ip.IPFrameEncapsulated

class IPEncapsulate<T : InternetProtocolFrame>(ipFrame: T) : PPPEncapsulate<IPFrameEncapsulated>(
	IPFrameEncapsulated(ipFrame)
)