package bread_experts_group.protocol.ip

import bread_experts_group.Writable
import bread_experts_group.protocol.ip.v4.InternetProtocolFrame
import bread_experts_group.util.ToStringUtil.SmartToString
import bread_experts_group.util.write16
import bread_experts_group.util.writeInet
import java.io.ByteArrayOutputStream
import java.io.OutputStream

abstract class InternetProtocol(val protocol: ProtocolTypes) : SmartToString() {
	enum class ProtocolTypes(val code: Int) {
		INTERNET_CONTROL_MESSAGE_PROTOCOL(1),
		INTERNET_GROUP_MANAGEMENT_PROTOCOL(2),
		TRANSMISSION_CONTROL_PROTOCOL(6),
		USER_DATAGRAM_PROTOCOL(17),
		IPV6_ENCAPSULATION(41),
		INTERNET_CONTROL_MESSAGE_PROTOCOL_V6(58),
		OPEN_SHORTEST_PATH_FIRST(89),
		STREAM_CONTROL_TRANSMISSION_PROTOCOL(132);

		companion object {
			val mapping = entries.associateBy { it.code }
		}
	}

	abstract fun calculateLength(): Int
	abstract fun write(stream: OutputStream, packet: Writable)
	fun getPseudoHeader(packet: Writable): ByteArrayOutputStream = ByteArrayOutputStream().also {
		when (packet) {
			is InternetProtocolFrame<*> -> {
				it.writeInet(packet.source)
				it.writeInet(packet.destination)
				it.write(0)
				it.write(packet.data.protocol.code)
				it.write16(packet.data.calculateLength())
			}

			else -> throw UnsupportedOperationException(packet.toString())
		}
	}
}