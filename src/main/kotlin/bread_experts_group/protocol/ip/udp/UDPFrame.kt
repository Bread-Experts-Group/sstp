package bread_experts_group.protocol.ip.udp

import bread_experts_group.Writable
import bread_experts_group.protocol.ip.InternetProtocol
import bread_experts_group.protocol.ip.v4.InternetProtocolFrame
import bread_experts_group.protocol.ip.v4.InternetProtocolFrame.Companion.calculateChecksum
import bread_experts_group.util.read16
import bread_experts_group.util.write16
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream

/**
 * A User Datagram Protocol frame that runs on top of [InternetProtocolFrame].
 * @param sourcePort The source port (sender).
 * @param destPort The destination port (receiver).
 * @param checksum The checksum for this UDP packet (if read). This will be recomputed when sent.
 * @param udpData The data to send after the UDP header.
 * @author Miko Elbrecht
 * @since 1.0.0; 2025/03/19
 */
class UDPFrame(
	val sourcePort: Int,
	val destPort: Int,
	val checksum: Int,
	val udpData: ByteArray
) : InternetProtocol(ProtocolTypes.USER_DATAGRAM_PROTOCOL) {
	override fun calculateLength(): Int = 8 + udpData.size
	override fun write(stream: OutputStream, packet: Writable) {
		val realData = ByteArrayOutputStream().use {
			it.write16(sourcePort)
			it.write16(destPort)
			it.write16(calculateLength())
			it.write16(0) // Checksum written later
			it.write(udpData)
			it.toByteArray()
		}
		getPseudoHeader(packet).use {
			it.write(realData)
			val sum = calculateChecksum(it.toByteArray())
			realData[realData.size - udpData.size - 2] = (sum shr 8).toByte()
			realData[realData.size - udpData.size - 1] = sum.toByte()
		}
		stream.write(realData)
	}

	override fun gist(): String = "($sourcePort > $destPort), # DATA: [${udpData.size}]"

	companion object {
		fun read(stream: InputStream): UDPFrame = UDPFrame(
			stream.read16(), stream.read16(), stream.read16(),
			stream.read16().let { stream.readNBytes(it - 8) }
		)
	}
}