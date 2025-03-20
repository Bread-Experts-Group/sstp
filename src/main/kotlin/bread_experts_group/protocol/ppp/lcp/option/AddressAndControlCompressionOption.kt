package bread_experts_group.protocol.ppp.lcp.option

import java.io.InputStream

class AddressAndControlCompressionOption : LinkControlConfigurationOption(
	ConfigurationOptionType.ADDRESS_AND_CONTROL_FIELD_COMPRESSION
) {
	override fun calculateLength(): Int = 0x2

	companion object {
		fun read(stream: InputStream): AddressAndControlCompressionOption {
			return AddressAndControlCompressionOption()
		}
	}
}