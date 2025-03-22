package bread_experts_group.protocol.ipv4.udp

import bread_experts_group.protocol.ipv4.InternetProtocolFrame
import bread_experts_group.util.read16
import bread_experts_group.util.write16
import bread_experts_group.util.writeInet4
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.Inet4Address

/**
 * A User Datagram Protocol frame that runs on top of [InternetProtocolFrame].
 * @param sourcePort The source port (sender).
 * @param destPort The destination port (receiver).
 * @param checksum The checksum for this UDP packet (if read). This will be recomputed when sent.
 * @param data The data to send after the UDP header.
 * @author Miko Elbrecht
 * @since 1.0.0; 2025/03/19
 */
class UDPFrame(
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
	val checksum: Int,
	val data: ByteArray
) : InternetProtocolFrame(
	dscp, ecn, identification, flags, fragmentOffset, ttl,
	IPProtocol.USER_DATAGRAM_PROTOCOL,
	source, destination
) {
	override fun calculateLength(): Int = super.calculateLength() + (8 + data.size)
	override fun write(stream: OutputStream) {
		val pseudo = ByteArrayOutputStream()
		pseudo.writeInet4(super.source)
		pseudo.writeInet4(super.destination)
		pseudo.write(0)
		pseudo.write(super.protocol.code)
		pseudo.write16(8 + data.size)
		super.write(stream)
		val out = ByteArrayOutputStream()
		out.write16(sourcePort)
		out.write16(destPort)
		out.write16(8 + data.size)
		out.write16(0) // Checksum written later
		out.write(data)
		val realData = out.toByteArray()
		pseudo.write(realData)
		val sum = calculateChecksum(pseudo.toByteArray())
		realData[realData.size - data.size - 2] = (sum shr 8).toByte()
		realData[realData.size - data.size - 1] = sum.toByte()
		stream.write(realData)
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
			destination: Inet4Address
		): UDPFrame = UDPFrame(
			dscp, ecn, identification, flags, fragmentOffset, ttl, source, destination,
			stream.read16(), stream.read16(), stream.read16(),
			stream.read16().let {
				stream.readNBytes(it - 8)
			}
		)
	}
}