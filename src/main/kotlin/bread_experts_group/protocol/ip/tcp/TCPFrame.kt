package bread_experts_group.protocol.ip.tcp

import bread_experts_group.Writable
import bread_experts_group.protocol.ip.InternetProtocol
import bread_experts_group.protocol.ip.tcp.option.TCPOption
import bread_experts_group.protocol.ip.v4.InternetProtocolFrame.Companion.calculateChecksum
import bread_experts_group.util.read16
import bread_experts_group.util.read32
import bread_experts_group.util.write16
import bread_experts_group.util.write32
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream

class TCPFrame(
	val sourcePort: Int,
	val destPort: Int,
	val sequence: Int,
	val acknowledgementNumber: Int,
	val tcpFlags: List<TCPFlag>,
	val window: Int,
	@Suppress("unused") val checksum: Int,
	val urgentPointer: Int,
	val options: List<TCPOption>,
	val tcpData: ByteArray
) : InternetProtocol(ProtocolTypes.TRANSMISSION_CONTROL_PROTOCOL) {
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

	override fun calculateLength(): Int = 20 + options.sumOf { it.calculateLength() } + tcpData.size
	override fun write(stream: OutputStream, packet: Writable) {
		val optionSize = options.sumOf { it.calculateLength() }
		val realData = ByteArrayOutputStream().use {
			it.write16(sourcePort)
			it.write16(destPort)
			it.write32(sequence)
			it.write32(acknowledgementNumber)
			val dataOffset = 5 + (optionSize / 4)
			it.write(dataOffset shl 4)
			var flagsRaw = 0
			tcpFlags.forEach { flagsRaw = flagsRaw or it.position }
			it.write(flagsRaw)
			it.write16(window)
			it.write16(0) // Checksum written later
			it.write16(urgentPointer)
			options.forEach { o -> o.write(it) }
			it.write(tcpData)
			it.toByteArray()
		}
		getPseudoHeader(packet).use {
			it.write(realData)
			val sum = calculateChecksum(it.toByteArray())
			realData[realData.size - tcpData.size - optionSize - 4] = (sum shr 8).toByte()
			realData[realData.size - tcpData.size - optionSize - 3] = sum.toByte()
		}
		stream.write(realData)
	}

	override fun gist(): String = "($sourcePort > $destPort), SEQ: $sequence, ACK: $acknowledgementNumber, " +
			"# DATA: [${tcpData.size}], [${tcpFlags.joinToString(",")}], # OPT: [${options.size}]" + buildString {
		options.forEach {
			append("\n  ${it.gist()}")
		}
	}

	companion object {
		fun read(
			stream: InputStream,
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
				sourcePort, destPort, sequence, ackNum,
				tcpFlags,
				window, checksum, urgentPointer, options,
				stream.readNBytes(length - 20 - ((dataOffset - 5) * 4))
			)
		}
	}
}