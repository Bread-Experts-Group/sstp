package bread_experts_group.protocol.ppp.ccp

import bread_experts_group.protocol.ppp.NCPControlType
import bread_experts_group.protocol.ppp.ccp.option.CCPConfigurationOption
import java.io.InputStream

class CCPRequest(
	identifier: Int,
	options: List<CCPConfigurationOption>
) : CCPConfiguration(identifier, options, NCPControlType.CONFIGURE_REQUEST) {
	constructor(stream: InputStream, length: Int, identifier: Int) : this(
		identifier,
		readOpts(stream, length)
	)
}