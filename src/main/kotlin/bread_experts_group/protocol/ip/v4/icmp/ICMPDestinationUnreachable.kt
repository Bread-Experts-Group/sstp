package bread_experts_group.protocol.ip.v4.icmp

import bread_experts_group.Writable
import bread_experts_group.protocol.ip.v4.InternetProtocolFrame
import bread_experts_group.protocol.ip.v4.InternetProtocolFrame.Companion.calculateChecksum
import bread_experts_group.util.write32
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream

class ICMPDestinationUnreachable(
	val data: ByteArray,
	code: Int
) : ICMPFrame(
	ICMPType.DESTINATION_UNREACHABLE,
	code
) {
	override fun calculateLength(): Int = super.calculateLength() + 4 + data.size
	override fun write(stream: OutputStream, packet: Writable) {
		if (packet !is InternetProtocolFrame<*>) throw IllegalStateException("Unexpected packet: $packet")
		val realData = ByteArrayOutputStream().use {
			super.write(it, packet)
			it.write32(0)
			it.write(data)
			it.toByteArray()
		}
		val sum = calculateChecksum(realData)
		realData[realData.size - data.size - 6] = (sum shr 8).toByte()
		realData[realData.size - data.size - 5] = sum.toByte()
		stream.write(realData)
	}

	override fun icmpGist(): String = "# DATA: [${data.size}]"

	companion object {
		fun read(stream: InputStream, code: Int, length: Int): ICMPDestinationUnreachable = ICMPDestinationUnreachable(
			stream.readNBytes(length - 4),
			code
		)
	}
}