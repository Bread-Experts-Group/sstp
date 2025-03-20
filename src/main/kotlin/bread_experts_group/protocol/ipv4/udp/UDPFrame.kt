package bread_experts_group.protocol.ipv4.udp

import bread_experts_group.protocol.ipv4.IPFrame
import bread_experts_group.util.read16
import bread_experts_group.util.write16
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.Inet4Address

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
) : IPFrame(
	dscp, ecn, identification, flags, fragmentOffset, ttl,
	IPProtocol.USER_DATAGRAM_PROTOCOL,
	source, destination
) {
	override fun calculateLength(): Int = super.calculateLength() + (8 + data.size)
	override fun write(stream: OutputStream) {
		super.write(stream)
		val out = ByteArrayOutputStream()
		out.write16(sourcePort)
		out.write16(destPort)
		out.write16(8 + data.size)
		out.write16(0)
		out.write(data)
		val asData = out.toByteArray()
		val sum = calculateChecksum(asData)
		asData[asData.size - data.size - 1] = (sum shr 8).toByte()
		asData[asData.size - data.size - 2] = sum.toByte()
		stream.write(asData)
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