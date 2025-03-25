package bread_experts_group.protocol.ip.v4.icmp

import bread_experts_group.Writable
import bread_experts_group.protocol.ip.v4.InternetProtocolFrame
import bread_experts_group.protocol.ip.v4.InternetProtocolFrame.Companion.calculateChecksum
import bread_experts_group.util.read16
import bread_experts_group.util.write16
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream

class ICMPEcho(
	val echoIdentifier: Int,
	val echoSequence: Int,
	val data: ByteArray,
	request: Boolean
) : ICMPFrame(
	if (request) ICMPType.ECHO_REQUEST else ICMPType.ECHO_REPLY,
	0
) {
	override fun calculateLength(): Int = super.calculateLength() + 4 + data.size
	override fun write(stream: OutputStream, packet: Writable) {
		if (packet !is InternetProtocolFrame<*>) throw IllegalStateException("Unexpected packet: $packet")
		val realData = ByteArrayOutputStream().use {
			super.write(it, packet)
			it.write16(echoIdentifier)
			it.write16(echoSequence)
			it.write(data)
			it.toByteArray()
		}
		val sum = calculateChecksum(realData)
		realData[realData.size - data.size - 6] = (sum shr 8).toByte()
		realData[realData.size - data.size - 5] = sum.toByte()
		stream.write(realData)
	}

	override fun icmpGist(): String = "ID: $echoIdentifier, SEQ: $echoSequence, # DATA: [${data.size}]"

	companion object {
		fun read(stream: InputStream, length: Int, request: Boolean): ICMPEcho = ICMPEcho(
			stream.read16(), stream.read16(), stream.readNBytes(length - 4),
			request
		)
	}
}