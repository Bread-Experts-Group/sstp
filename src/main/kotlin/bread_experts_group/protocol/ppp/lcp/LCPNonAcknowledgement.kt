package bread_experts_group.protocol.ppp.lcp

import bread_experts_group.protocol.ppp.lcp.option.LCPConfigurationOption
import java.io.InputStream

class LCPNonAcknowledgement(
	options: List<LCPConfigurationOption>,
	identifier: Int
) : LCPConfiguration(identifier, options, LCPControlType.CONFIGURE_NAK) {
	constructor(stream: InputStream, length: Int, identifier: Int) : this(
		readOpts(stream, length),
		identifier
	)
}