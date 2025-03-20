package bread_experts_group.protocol.ppp.lcp.option

import java.io.InputStream

class ProtocolFieldCompressionOption : LinkControlConfigurationOption(ConfigurationOptionType.PROTOCOL_FIELD_COMPRESSION) {
	override fun calculateLength(): Int = 0x2

	companion object {
		fun read(stream: InputStream): ProtocolFieldCompressionOption {
			return ProtocolFieldCompressionOption()
		}
	}
}