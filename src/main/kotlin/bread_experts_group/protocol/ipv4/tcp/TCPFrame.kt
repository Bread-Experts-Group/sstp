package bread_experts_group.protocol.ipv4.tcp

import bread_experts_group.protocol.ipv4.InternetProtocolFrame
import bread_experts_group.protocol.ipv4.tcp.option.TCPOption
import bread_experts_group.util.*
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.Inet4Address

class TCPFrame(
	dscp: Int,
	ecn: Int,
	identification: Int,
	flags: List<IPFlag>,
	fragmentOffset: Int,
	ttl: Int,
	source: Inet4Address,
	destination: Inet4Address,
	val sourcePort: Int,
	val destPort: Int,
	val sequence: Int,
	val acknowledgementNumber: Int,
	val tcpFlags: List<TCPFlag>,
	val window: Int,
	@Suppress("unused") val checksum: Int,
	val urgentPointer: Int,
	val options: List<TCPOption>,
	val data: ByteArray
) : InternetProtocolFrame(
	dscp, ecn, identification, flags, fragmentOffset, ttl,
	IPProtocol.TRANSMISSION_CONTROL_PROTOCOL,
	source, destination
) {
	fun tcpLength() = 20 + options.sumOf { it.calculateLength() } + data.size
	override fun calculateLength(): Int = super.calculateLength() + tcpLength()
	override fun write(stream: OutputStream) {
		val pseudo = ByteArrayOutputStream()
		pseudo.writeInet4(super.source)
		pseudo.writeInet4(super.destination)
		pseudo.write(0)
		pseudo.write(super.protocol.code)
		pseudo.write16(tcpLength())
		super.write(stream)
		val out = ByteArrayOutputStream()
		out.write16(sourcePort)
		out.write16(destPort)
		out.write32(sequence)
		out.write32(acknowledgementNumber)
		val optionSize = options.sumOf { it.calculateLength() }
		val dataOffset = 5 + (optionSize / 4)
		out.write(dataOffset shl 4)
		var flagsRaw = 0
		tcpFlags.forEach { flagsRaw = flagsRaw or it.position }
		out.write(flagsRaw)
		out.write16(window)
		out.write16(0) // Checksum written later
		out.write16(urgentPointer)
		options.forEach { it.write(out) }
		out.write(data)
		val realData = out.toByteArray()
		pseudo.write(realData)
		val sum = calculateChecksum(pseudo.toByteArray())
		realData[realData.size - data.size - optionSize - 4] = (sum shr 8).toByte()
		realData[realData.size - data.size - optionSize - 3] = sum.toByte()
		stream.write(realData)
	}

	enum class TCPFlag(val position: Int) {
		CWR(0b10000000),
		EHE(0b01000000),
		URG(0b00100000),
		ACK(0b00010000),
		PSH(0b00001000),
		RST(0b00000100),
		SYN(0b00000010),
		FIN(0b00000001)
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
		): TCPFrame {
			val sourcePort = stream.read16()
			val destPort = stream.read16()
			val sequence = stream.read32()
			val ackNum = stream.read32()
			val dataOffset = stream.read() shr 4 // todo > 5
			val tcpFlags = stream.read().let { TCPFlag.entries.filter { c -> (it and c.position) > 0 } }
			val window = stream.read16()
			val checksum = stream.read16()
			val urgentPointer = stream.read16()
			val options = buildList {
				var remaining = (dataOffset - 5) * 4
				while (remaining > 0) {
					val option = TCPOption.read(stream)
					add(option)
					remaining -= option.calculateLength()
				}
			}
			return TCPFrame(
				dscp, ecn, identification, flags, fragmentOffset, ttl, source, destination,
				sourcePort, destPort, sequence, ackNum,
				tcpFlags,
				window, checksum, urgentPointer, options,
				stream.readNBytes(length - 20 - ((dataOffset - 5) * 4))
			)
		}
	}
}