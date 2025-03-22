package bread_experts_group.protocol.ppp

import bread_experts_group.Writable
import bread_experts_group.protocol.ppp.ccp.CompressionControlProtocolFrame
import bread_experts_group.protocol.ppp.ip.IPFrameEncapsulated
import bread_experts_group.protocol.ppp.ipcp.InternetProtocolControlProtocolFrame
import bread_experts_group.protocol.ppp.ipv6cp.InternetProtocolV6ControlProtocolFrame
import bread_experts_group.protocol.ppp.lcp.LinkControlProtocolFrame
import bread_experts_group.protocol.ppp.pap.PasswordAuthenticationProtocolFrame
import bread_experts_group.util.ToStringUtil.SmartToString
import bread_experts_group.util.read16
import bread_experts_group.util.write16
import java.io.InputStream
import java.io.OutputStream

abstract class PointToPointProtocolFrame internal constructor(
	val protocol: PPPProtocol
) : SmartToString(), Writable {
	override fun calculateLength(): Int = 4

	enum class PPPProtocol(val code: Int) {
		INTERNET_PROTOCOL_V4(0x0021),
		LINK_CONTROL_PROTOCOL(0xC021),
		PASSWORD_AUTHENTICATION_PROTOCOL(0xC023),
		INTERNET_PROTOCOL_CONTROL_PROTOCOL(0x8021),
		INTERNET_PROTOCOL_V6_CONTROL_PROTOCOL(0x8057),
		COMPRESSION_CONTROL_PROTOCOL(0x80FD);

		companion object {
			val mapping = entries.associateBy { it.code }
		}
	}

	override fun write(stream: OutputStream) {
		// TODO, the LCP compression options.
		stream.write(0xFF)
		stream.write(0x03)
		stream.write16(this.protocol.code)
	}

	companion object {
		fun read(stream: InputStream): PointToPointProtocolFrame {
			stream.read() // broadcastAddress
			stream.read() // unnumberedData
			val protocol = PPPProtocol.mapping.getValue(stream.read16())
			return when (protocol) {
				PPPProtocol.INTERNET_PROTOCOL_V4 -> IPFrameEncapsulated.read(stream)
				PPPProtocol.PASSWORD_AUTHENTICATION_PROTOCOL -> PasswordAuthenticationProtocolFrame.read(stream)
				PPPProtocol.LINK_CONTROL_PROTOCOL -> LinkControlProtocolFrame.read(stream)
				PPPProtocol.COMPRESSION_CONTROL_PROTOCOL -> CompressionControlProtocolFrame.read(stream)
				PPPProtocol.INTERNET_PROTOCOL_CONTROL_PROTOCOL -> InternetProtocolControlProtocolFrame.read(stream)
				PPPProtocol.INTERNET_PROTOCOL_V6_CONTROL_PROTOCOL -> InternetProtocolV6ControlProtocolFrame.read(stream)
			}
		}
	}
}