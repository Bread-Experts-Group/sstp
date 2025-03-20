package bread_experts_group.protocol.ppp.ipv6cp.option

import bread_experts_group.util.read64
import java.io.InputStream

class InterfaceIdentifierConfigurationOption(
	val identifier: Long
) : InternetProtocolV6ControlConfigurationOption(InternetProtocolV6OptionType.INTERFACE_IDENTIFIER) {
	override fun calculateLength(): Int = 10

	companion object {
		fun read(stream: InputStream): InterfaceIdentifierConfigurationOption = InterfaceIdentifierConfigurationOption(
			stream.read64()
		)
	}
}