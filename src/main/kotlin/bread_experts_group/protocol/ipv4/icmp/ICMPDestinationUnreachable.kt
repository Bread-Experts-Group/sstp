package bread_experts_group.protocol.ipv4.icmp

import bread_experts_group.util.write32
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.Inet4Address

class ICMPDestinationUnreachable(
	dscp: Int,
	ecn: Int,
	identification: Int,
	flags: List<IPFlag>,
	fragmentOffset: Int,
	ttl: Int,
	source: Inet4Address,
	destination: Inet4Address,
	val data: ByteArray,
	code: Int
) : ICMPFrame(
	dscp, ecn, identification, flags, fragmentOffset, ttl, source, destination,
	ICMPType.DESTINATION_UNREACHABLE,
	code
) {
	override fun calculateLength(): Int = super.calculateLength() + 4 + data.size
	override fun write(stream: OutputStream) {
		val out = ByteArrayOutputStream()
		super.write(out)
		out.write32(0)
		out.write(data)
		val asData = out.toByteArray()
		val sum = calculateChecksum(asData)
		asData[asData.size - data.size - 6] = (sum shr 8).toByte()
		asData[asData.size - data.size - 5] = sum.toByte()
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
			destination: Inet4Address,
			code: Int,
			length: Int
		): ICMPDestinationUnreachable = ICMPDestinationUnreachable(
			dscp, ecn, identification, flags, fragmentOffset, ttl, source, destination.also { stream.skip(4) },
			stream.readNBytes(length - 4),
			code
		)
	}
}