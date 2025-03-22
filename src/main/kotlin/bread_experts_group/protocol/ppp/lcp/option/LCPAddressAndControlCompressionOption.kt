package bread_experts_group.protocol.ppp.lcp.option

class LCPAddressAndControlCompressionOption : LCPConfigurationOption(
	ConfigurationOptionType.ADDRESS_AND_CONTROL_FIELD_COMPRESSION
) {
	override fun calculateLength(): Int = 0x2

	companion object {
		fun read(): LCPAddressAndControlCompressionOption {
			return LCPAddressAndControlCompressionOption()
		}
	}
}