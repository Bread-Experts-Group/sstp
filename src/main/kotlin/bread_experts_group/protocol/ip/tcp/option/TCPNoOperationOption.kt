package bread_experts_group.protocol.ip.tcp.option

class TCPNoOperationOption : TCPOption(TCPOptionType.NO_OPERATION) {
	override fun calculateLength(): Int = 1

	override fun optionGist(): String = "<>"

	companion object {
		fun read() = TCPNoOperationOption()
	}
}