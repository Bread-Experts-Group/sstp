package bread_experts_group.protocol.ip.tcp.option

import bread_experts_group.util.read32
import bread_experts_group.util.write32
import java.io.InputStream
import java.io.OutputStream

class TCPLastTimestampsOption(val sender: Int, val lastMine: Int) : TCPOption(TCPOptionType.LAST_TIMESTAMPS) {
	override fun calculateLength(): Int = 10
	override fun write(stream: OutputStream) {
		super.write(stream)
		stream.write32(sender)
		stream.write32(lastMine)
	}

	override fun optionGist(): String = "SENDER: $sender, LAST MINE: $lastMine"

	companion object {
		fun read(stream: InputStream) = TCPLastTimestampsOption(stream.read32(), stream.read32())
	}
}