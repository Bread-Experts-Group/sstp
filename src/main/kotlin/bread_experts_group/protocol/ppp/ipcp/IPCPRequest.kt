package bread_experts_group.protocol.ppp.ipcp

import bread_experts_group.protocol.ppp.NCPControlType
import bread_experts_group.protocol.ppp.ipcp.option.IPCPConfigurationOption
import java.io.InputStream

class IPCPRequest(
	options: List<IPCPConfigurationOption>,
	identifier: Int
) : IPCPConfiguration(identifier, options, NCPControlType.CONFIGURE_REQUEST) {
	constructor(stream: InputStream, length: Int, identifier: Int) : this(
		readOpts(stream, length),
		identifier
	)
}