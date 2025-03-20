package bread_experts_group.protocol.ipv4.tcp.option

import java.io.OutputStream

class TCPSensitiveAcknowledgementAllowedOption : TCPOption(TCPOptionType.MAXIMUM_SEGMENT_SIZE) {
	override fun calculateLength(): Int = 2
	override fun write(stream: OutputStream) {
		TODO("Not yet implemented")
	}

	companion object {
		fun read() = TCPSensitiveAcknowledgementAllowedOption()
	}
}