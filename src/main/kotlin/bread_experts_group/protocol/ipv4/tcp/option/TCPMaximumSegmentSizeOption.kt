package bread_experts_group.protocol.ipv4.tcp.option

import bread_experts_group.util.read16
import bread_experts_group.util.write16
import java.io.InputStream
import java.io.OutputStream

class TCPMaximumSegmentSizeOption(val size: Int) : TCPOption(TCPOptionType.MAXIMUM_SEGMENT_SIZE) {
	override fun calculateLength(): Int = 4
	override fun write(stream: OutputStream) {
		super.write(stream)
		stream.write16(size)
	}

	companion object {
		fun read(stream: InputStream) = TCPMaximumSegmentSizeOption(stream.read16())
	}
}