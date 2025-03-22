package bread_experts_group.protocol.ipv4.icmp

import bread_experts_group.util.read16
import bread_experts_group.util.write16
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.Inet4Address

class ICMPEcho(
	dscp: Int,
	ecn: Int,
	identification: Int,
	flags: List<IPFlag>,
	fragmentOffset: Int,
	ttl: Int,
	source: Inet4Address,
	destination: Inet4Address,
	val echoIdentifier: Int,
	val echoSequence: Int,
	val data: ByteArray,
	request: Boolean
) : ICMPFrame(
	dscp, ecn, identification, flags, fragmentOffset, ttl, source, destination,
	if (request) ICMPType.ECHO_REQUEST else ICMPType.ECHO_REPLY,
	0
) {
	override fun calculateLength(): Int = super.calculateLength() + 4 + data.size
	override fun write(stream: OutputStream) {
		val out = ByteArrayOutputStream()
		super.write(out)
		out.write16(echoIdentifier)
		out.write16(echoSequence)
		out.write(data)
		val asData = out.toByteArray()
		val sum = calculateChecksum(asData)
		asData[asData.size - data.size - 6] = (sum shr 8).toByte()
		asData[asData.size - data.size - 5] = sum.toByte()
		stream.write(asData)
	}

	override fun icmpGist(): String = "ID: $echoIdentifier, SEQ: $echoSequence, # DATA: [${data.size}]"

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
			length: Int,
			request: Boolean
		): ICMPEcho = ICMPEcho(
			dscp, ecn, identification, flags, fragmentOffset, ttl, source, destination,
			stream.read16(), stream.read16(), stream.readNBytes(length - 4),
			request
		)
	}
}