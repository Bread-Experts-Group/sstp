package bread_experts_group.protocol.ppp.ccp

import bread_experts_group.protocol.ppp.NCPControlType
import bread_experts_group.protocol.ppp.ccp.option.CCPConfigurationOption
import java.io.InputStream

class CCPAcknowledgement(
	identifier: Int,
	options: List<CCPConfigurationOption>
) : CCPConfiguration(identifier, options, NCPControlType.CONFIGURE_ACK) {
	constructor(stream: InputStream, length: Int, identifier: Int) : this(
		identifier,
		readOpts(stream, length)
	)
}