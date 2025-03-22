package bread_experts_group.protocol.ipv4.tcp.option

class TCPSensitiveAcknowledgementAllowedOption : TCPOption(TCPOptionType.MAXIMUM_SEGMENT_SIZE) {
	override fun calculateLength(): Int = 2

	override fun optionGist(): String = "<>"

	companion object {
		fun read() = TCPSensitiveAcknowledgementAllowedOption()
	}
}