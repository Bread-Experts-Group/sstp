package bread_experts_group.protocol.ppp.lcp.option

class LCPProtocolFieldCompressionOption : LCPConfigurationOption(ConfigurationOptionType.PROTOCOL_FIELD_COMPRESSION) {
	override fun calculateLength(): Int = 0x2
	override fun optionGist(): String = "<>"

	companion object {
		fun read(): LCPProtocolFieldCompressionOption {
			return LCPProtocolFieldCompressionOption()
		}
	}
}