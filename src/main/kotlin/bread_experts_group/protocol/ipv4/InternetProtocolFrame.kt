package bread_experts_group.protocol.ipv4

import bread_experts_group.Writable
import bread_experts_group.protocol.ipv4.icmp.ICMPFrame
import bread_experts_group.protocol.ipv4.tcp.TCPFrame
import bread_experts_group.protocol.ipv4.udp.UDPFrame
import bread_experts_group.util.ToStringUtil.SmartToString
import bread_experts_group.util.read16
import bread_experts_group.util.readInet4
import bread_experts_group.util.write16
import bread_experts_group.util.writeInet4
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.Inet4Address

abstract class InternetProtocolFrame(
	val dscp: Int,
	val ecn: Int,
	val identification: Int,
	val flags: List<IPFlag>,
	val fragmentOffset: Int,
	val ttl: Int,
	val protocol: IPProtocol,
	val source: Inet4Address,
	val destination: Inet4Address
) : SmartToString(), Writable {
	override fun calculateLength(): Int = 20

	override fun write(stream: OutputStream) {
		val out = ByteArrayOutputStream()
		out.write((4 shl 4) or 0x5)
		out.write(ecn or (dscp shl 2))
		out.write16(calculateLength())
		out.write16(identification)
		var flagsRaw = 0
		flags.forEach { flagsRaw = flagsRaw or it.position }
		out.write16((flagsRaw shl 13) or fragmentOffset)
		out.write(ttl)
		out.write(protocol.code)
		out.write16(0)
		out.writeInet4(source)
		out.writeInet4(destination)
		val asData = out.toByteArray()
		val sum = calculateChecksum(asData)
		asData[asData.size - 10] = (sum shr 8).toByte()
		asData[asData.size - 9] = sum.toByte()
		stream.write(asData)
	}

	enum class IPFlag(val position: Int) {
		DONT_FRAGMENT(0b010),
		MORE_FRAGMENTS(0b001)
	}

	enum class IPProtocol(val code: Int) {
		INTERNET_CONTROL_MESSAGE_PROTOCOL(1),
		INTERNET_GROUP_MANAGEMENT_PROTOCOL(2),
		TRANSMISSION_CONTROL_PROTOCOL(6),
		USER_DATAGRAM_PROTOCOL(17),
		IPV6_ENCAPSULATION(41),
		OPEN_SHORTEST_PATH_FIRST(89),
		STREAM_CONTROL_TRANSMISSION_PROTOCOL(132);

		companion object {
			val mapping = entries.associateBy { it.code }
		}
	}

	companion object {
		fun calculateChecksum(data: ByteArray): Int {
			var sum = 0
			for (i in data.indices step 2) {
				val word = ((data[i].toInt() and 0xFF) shl 8) or (data.getOrElse(i + 1) { 0 }.toInt() and 0xFF)
				sum += word
				if (sum > 0xFFFF) sum = (sum and 0xFFFF) + (sum shr 16)
			}
			return sum.inv() and 0xFFFF
		}

		fun read(stream: InputStream): InternetProtocolFrame {
			val (version, ihl) = stream.read().let { (it shr 4) to (it and 0xF) }
			if (version != 4) throw IllegalStateException(version.toString())
			if (ihl > 5) TODO("Internet Protocol options")
			val (dscp, ecn) = stream.read().let { (it shr 2) to (it and 0x3) }
			val totalLength = stream.read16()
			val identification = stream.read16()
			val (flagsRaw, fragmentOffset) = stream.read16().let { (it shr 13) to (it and 0x1FFF) }
			val flags = IPFlag.entries.filter { (flagsRaw and it.position) > 0 }
			val ttl = stream.read()
			val protocol = IPProtocol.mapping.getValue(stream.read())
			stream.read16() // checksum
			return when (protocol) {
				IPProtocol.INTERNET_CONTROL_MESSAGE_PROTOCOL -> ICMPFrame.read(
					stream,
					dscp, ecn, identification, flags, fragmentOffset, ttl,
					stream.readInet4(),
					stream.readInet4(),
					totalLength - 20
				)

				IPProtocol.TRANSMISSION_CONTROL_PROTOCOL -> TCPFrame.read(
					stream,
					dscp, ecn, identification, flags, fragmentOffset, ttl,
					stream.readInet4(),
					stream.readInet4(),
					totalLength - 20
				)

				IPProtocol.USER_DATAGRAM_PROTOCOL -> UDPFrame.read(
					stream,
					dscp, ecn, identification, flags, fragmentOffset, ttl,
					stream.readInet4(),
					stream.readInet4()
				)

				else -> TODO(protocol.toString())
			}
		}
	}
}