package bread_experts_group.protocol.ppp.ipv6cp

import bread_experts_group.protocol.ppp.NCPControlType
import bread_experts_group.protocol.ppp.ipv6cp.option.IPv6CPConfigurationOption
import java.io.InputStream

class IPv6CPNonAcknowledgement(
	options: List<IPv6CPConfigurationOption>,
	identifier: Int
) : IPv6CPConfiguration(identifier, options, NCPControlType.CONFIGURE_NAK) {
	constructor(stream: InputStream, length: Int, identifier: Int) : this(
		readOpts(stream, length),
		identifier
	)
}