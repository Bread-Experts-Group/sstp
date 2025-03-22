package bread_experts_group.protocol.ppp.ipv6cp.option

import bread_experts_group.util.read64
import java.io.InputStream

class IPv6CPInterfaceIdentifierOption(
	val identifier: Long
) : IPv6CPConfigurationOption(InternetProtocolV6OptionType.INTERFACE_IDENTIFIER) {
	override fun calculateLength(): Int = 10

	companion object {
		fun read(stream: InputStream): IPv6CPInterfaceIdentifierOption = IPv6CPInterfaceIdentifierOption(stream.read64())
	}
}