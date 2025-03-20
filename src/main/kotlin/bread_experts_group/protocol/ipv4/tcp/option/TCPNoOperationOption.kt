package bread_experts_group.protocol.ipv4.tcp.option

import java.io.OutputStream

class TCPNoOperationOption : TCPOption(TCPOptionType.NO_OPERATION) {
	override fun calculateLength(): Int = 1
	override fun write(stream: OutputStream) {
		TODO("Not yet implemented")
	}

	companion object {
		fun read() = TCPNoOperationOption()
	}
}