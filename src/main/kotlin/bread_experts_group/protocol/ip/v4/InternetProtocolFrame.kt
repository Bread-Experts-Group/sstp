package bread_experts_group.protocol.ip.v4

import bread_experts_group.Writable
import bread_experts_group.protocol.ip.InternetProtocol
import bread_experts_group.protocol.ip.InternetProtocol.ProtocolTypes
import bread_experts_group.protocol.ip.tcp.TCPFrame
import bread_experts_group.protocol.ip.udp.UDPFrame
import bread_experts_group.protocol.ip.v4.icmp.ICMPFrame
import bread_experts_group.util.ToStringUtil.SmartToString
import bread_experts_group.util.read16
import bread_experts_group.util.readInet4
import bread_experts_group.util.write16
import bread_experts_group.util.writeInet
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.Inet4Address

class InternetProtocolFrame<T : InternetProtocol>(
	val differentiatedServices: Int,
	val congestionNotification: Int,
	val identification: Int,
	val flags: List<IPFlag>,
	val fragmentOffset: Int,
	val timeToLive: Int,
	val source: Inet4Address,
	val destination: Inet4Address,
	val data: T
) : SmartToString(), Writable {
	enum class IPFlag(val position: Int) {
		DONT_FRAGMENT(0b010),
		MORE_FRAGMENTS(0b001)
	}

	override fun calculateLength(): Int = 20 + data.calculateLength()
	override fun write(stream: OutputStream) {
		val realData = ByteArrayOutputStream().use {
			it.write((4 shl 4) or 0x5)
			it.write(congestionNotification or (differentiatedServices shl 2))
			it.write16(calculateLength())
			it.write16(identification)
			var flagsRaw = 0
			flags.forEach { flagsRaw = flagsRaw or it.position }
			it.write16((flagsRaw shl 13) or fragmentOffset)
			it.write(timeToLive)
			it.write(data.protocol.code)
			it.write16(0)
			it.writeInet(source)
			it.writeInet(destination)
			it.toByteArray()
		}
		val sum = calculateChecksum(realData)
		realData[realData.size - 10] = (sum shr 8).toByte()
		realData[realData.size - 9] = sum.toByte()
		stream.write(
			realData + ByteArrayOutputStream().use {
				data.write(it, this)
				it.toByteArray()
			}
		)
	}

	override fun gist(): String = "${data.protocol} ($source > $destination), TTL: $timeToLive, ID: $identification, " +
			"[${flags.joinToString(",")}]\n${data.gist()}"

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

		fun read(stream: InputStream): InternetProtocolFrame<*> {
			val (version, ihl) = stream.read().let { (it shr 4) to (it and 0xF) }
			if (version != 4) throw IllegalStateException(version.toString())
			if (ihl > 5) TODO("Internet Protocol options")
			val (differentiatedServices, congestionNotification) = stream.read().let { (it shr 2) to (it and 0x3) }
			val length = stream.read16() - 20
			val identification = stream.read16()
			val (flagsRaw, fragmentOffset) = stream.read16().let { (it shr 13) to (it and 0x1FFF) }
			val flags = IPFlag.entries.filter { (flagsRaw and it.position) > 0 }
			val ttl = stream.read()
			val protocol = ProtocolTypes.mapping.getValue(stream.read())
			stream.read16() // checksum
			return InternetProtocolFrame(
				differentiatedServices,
				congestionNotification,
				identification,
				flags,
				fragmentOffset,
				ttl,
				stream.readInet4(),
				stream.readInet4(),
				when (protocol) {
					ProtocolTypes.USER_DATAGRAM_PROTOCOL -> UDPFrame.read(stream)
					ProtocolTypes.TRANSMISSION_CONTROL_PROTOCOL -> TCPFrame.read(stream, length)
					ProtocolTypes.INTERNET_CONTROL_MESSAGE_PROTOCOL -> ICMPFrame.read(stream, length)
					else -> throw UnsupportedOperationException(protocol.name)
				}
			)
		}
	}
}