package bread_experts_group.protocol.ppp.lcp.option

class LCPProtocolFieldCompressionOption : LCPConfigurationOption(ConfigurationOptionType.PROTOCOL_FIELD_COMPRESSION) {
	override fun calculateLength(): Int = 0x2

	companion object {
		fun read(): LCPProtocolFieldCompressionOption {
			return LCPProtocolFieldCompressionOption()
		}
	}
}