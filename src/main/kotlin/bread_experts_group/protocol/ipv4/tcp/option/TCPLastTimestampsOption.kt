package bread_experts_group.protocol.ipv4.tcp.option

import bread_experts_group.util.read32
import java.io.InputStream
import java.io.OutputStream

class TCPLastTimestampsOption(val sender: Int, val lastMine: Int) : TCPOption(TCPOptionType.LAST_TIMESTAMPS) {
	override fun calculateLength(): Int = 10
	override fun write(stream: OutputStream) {
		TODO("Not yet implemented")
	}

	companion object {
		fun read(stream: InputStream) = TCPLastTimestampsOption(stream.read32(), stream.read32())
	}
}