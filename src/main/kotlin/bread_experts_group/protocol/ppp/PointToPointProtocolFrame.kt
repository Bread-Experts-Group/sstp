package bread_experts_group.protocol.ppp

import bread_experts_group.Writable
import bread_experts_group.protocol.ppp.ccp.CompressionControlProtocolFrame
import bread_experts_group.protocol.ppp.ip.IPFrameEncapsulated
import bread_experts_group.protocol.ppp.ip.IPv6FrameEncapsulated
import bread_experts_group.protocol.ppp.ipcp.InternetProtocolControlProtocolFrame
import bread_experts_group.protocol.ppp.ipv6cp.InternetProtocolV6ControlProtocolFrame
import bread_experts_group.protocol.ppp.lcp.LinkControlProtocolFrame
import bread_experts_group.protocol.ppp.pap.PasswordAuthenticationProtocolFrame
import bread_experts_group.util.ToStringUtil.SmartToString
import bread_experts_group.util.write16
import java.io.InputStream
import java.io.OutputStream

abstract class PointToPointProtocolFrame internal constructor(
	val protocol: PPPProtocol
) : SmartToString(), Writable {
	override fun calculateLength(): Int = 4

	enum class PPPProtocol(val code: Int) {
		INTERNET_PROTOCOL_V4(0x0021),
		INTERNET_PROTOCOL_V6(0x0057),
		COMPRESSED_DATAGRAM(0x00FD),
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
		if (!compression.outbound.addressAndControl) {
			stream.write(0xFF)
			stream.write(0x03)
		}
		(if (compression.outbound.protocol && this.protocol.code < 256) stream::write else stream::write16)(this.protocol.code)
	}

	final override fun gist(): String = "PPP [${calculateLength()}] $protocol\n${protocolGist()}"
	abstract fun protocolGist(): String

	companion object {
		val compressions = mutableMapOf<Thread, PPPCompressionDirections>()
		val compression: PPPCompressionDirections
			get() = compressions.getOrPut(Thread.currentThread()) { PPPCompressionDirections() }

		data class PPPCompressionDirections(
			val inbound: PPPCompressionObject = PPPCompressionObject(),
			val outbound: PPPCompressionObject = PPPCompressionObject()
		)

		data class PPPCompressionObject(
			var protocol: Boolean = false,
			var addressAndControl: Boolean = false
		)

		fun read(stream: InputStream): PointToPointProtocolFrame {
			var nextByte = stream.read()
			if (nextByte == 0xFF) {
				stream.read()
				nextByte = stream.read()
			}
			val protocol = PPPProtocol.mapping.getValue(
				if (compression.inbound.protocol) {
					nextByte.let { if (it and 1 == 1) it else ((it shl 8) or stream.read()) }
				} else ((nextByte shl 8) or stream.read())
			)
			return when (protocol) {
				PPPProtocol.INTERNET_PROTOCOL_V4 -> IPFrameEncapsulated.read(stream)
				PPPProtocol.INTERNET_PROTOCOL_V6 -> IPv6FrameEncapsulated.read(stream)
				PPPProtocol.PASSWORD_AUTHENTICATION_PROTOCOL -> PasswordAuthenticationProtocolFrame.read(stream)
				PPPProtocol.LINK_CONTROL_PROTOCOL -> LinkControlProtocolFrame.read(stream)
				PPPProtocol.COMPRESSION_CONTROL_PROTOCOL -> CompressionControlProtocolFrame.read(stream)
				PPPProtocol.INTERNET_PROTOCOL_CONTROL_PROTOCOL -> InternetProtocolControlProtocolFrame.read(stream)
				PPPProtocol.INTERNET_PROTOCOL_V6_CONTROL_PROTOCOL -> InternetProtocolV6ControlProtocolFrame.read(stream)
				PPPProtocol.COMPRESSED_DATAGRAM -> TODO("CCP Stabilization") /*read(
					compressionHandler(ByteArrayInputStream(stream.readAllBytes()))
				)*/
			}
		}
	}
}