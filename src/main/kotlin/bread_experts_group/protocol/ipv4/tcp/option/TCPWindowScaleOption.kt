package bread_experts_group.protocol.ipv4.tcp.option

import java.io.InputStream
import java.io.OutputStream

class TCPWindowScaleOption(val scale: Int) : TCPOption(TCPOptionType.WINDOW_SCALE) {
	override fun calculateLength(): Int = 3
	override fun write(stream: OutputStream) {
		TODO("Not yet implemented")
	}

	companion object {
		fun read(stream: InputStream) = TCPWindowScaleOption(stream.read())
	}
}