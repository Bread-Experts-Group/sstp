package bread_experts_group.protocol.ipv4.icmp

import bread_experts_group.protocol.ipv4.IPFrame
import bread_experts_group.util.read16
import bread_experts_group.util.write16
import java.io.InputStream
import java.io.OutputStream
import java.net.Inet4Address

sealed class ICMPFrame(
	dscp: Int,
	ecn: Int,
	identification: Int,
	flags: List<IPFlag>,
	fragmentOffset: Int,
	ttl: Int,
	source: Inet4Address,
	destination: Inet4Address,
	val type: ICMPType,
	val code: Int
) : IPFrame(
	dscp, ecn, identification, flags, fragmentOffset, ttl,
	IPProtocol.INTERNET_CONTROL_MESSAGE_PROTOCOL,
	source, destination
) {
	override fun calculateLength(): Int = super.calculateLength() + 4
	override fun write(stream: OutputStream) {
		super.write(stream)
		stream.write(type.code)
		stream.write(code)
		stream.write16(0) // TODO
	}

	enum class ICMPType(val code: Int) {
		ECHO_REPLY(0),
		DESTINATION_UNREACHABLE(3),
		SOURCE_QUENCH(4),
		REDIRECT_MESSAGE(5),
		ECHO_REQUEST(8),
		ROUTER_ADVERTISEMENT(9),
		ROUTER_SOLICITATION(10),
		TIME_EXCEEDED(11),
		PARAMETER_PROBLEM_BAD_IP_HEADER(12),
		TIMESTAMP(13),
		TIMESTAMP_REPLY(14),
		EXTENDED_ECHO_REQUEST(42),
		EXTENDED_ECHO_REPLY(43);

		companion object {
			val mapping = entries.associateBy { it.code }
		}
	}

	companion object {
		fun read(
			stream: InputStream,
			dscp: Int,
			ecn: Int,
			identification: Int,
			flags: List<IPFlag>,
			fragmentOffset: Int,
			ttl: Int,
			source: Inet4Address,
			destination: Inet4Address,
			length: Int
		): ICMPFrame {
			val type = ICMPType.mapping.getValue(stream.read())
			val code = stream.read()
			stream.read16() // checksum
			return when (type) {
				ICMPType.ECHO_REQUEST, ICMPType.ECHO_REPLY -> ICMPEcho.read(
					stream, dscp, ecn, identification, flags, fragmentOffset, ttl, source, destination,
					length - 4,
					type == ICMPType.ECHO_REQUEST
				)

				ICMPType.DESTINATION_UNREACHABLE -> ICMPDestinationUnreachable.read(
					stream, dscp, ecn, identification, flags, fragmentOffset, ttl, source, destination,
					code, length - 4
				)

				else -> TODO(type.toString())
			}
		}
	}
}