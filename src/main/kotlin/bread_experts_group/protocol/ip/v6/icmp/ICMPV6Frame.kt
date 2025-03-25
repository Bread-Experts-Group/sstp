package bread_experts_group.protocol.ip.v6.icmp

import bread_experts_group.Writable
import bread_experts_group.protocol.ip.InternetProtocol
import bread_experts_group.util.read16
import bread_experts_group.util.write16
import java.io.InputStream
import java.io.OutputStream

sealed class ICMPV6Frame(
	val type: ICMPV6Type,
	val code: Int
) : InternetProtocol(ProtocolTypes.INTERNET_CONTROL_MESSAGE_PROTOCOL_V6) {
	enum class ICMPV6Type(val code: Int) {
		ROUTER_SOLICITATION(133);

		companion object {
			val mapping = entries.associateBy { it.code }
		}
	}

	override fun calculateLength(): Int = 4
	override fun write(stream: OutputStream, packet: Writable) {
		stream.write(type.code)
		stream.write(code)
		stream.write16(0)
	}

	final override fun gist(): String = "$type ($code) : ${icmpGist()}"
	abstract fun icmpGist(): String

	companion object {
		fun read(stream: InputStream, length: Int): ICMPV6Frame {
			val type = ICMPV6Type.mapping.getValue(stream.read())
			stream.read() // code
			stream.read16() // checksum
			val length = length - 4
			return when (type) {
				ICMPV6Type.ROUTER_SOLICITATION -> ICMPV6RouterSolicitation.read(stream, length)
			}
		}
	}
}