package bread_experts_group.protocol.ppp.ccp.option

import java.io.InputStream

class CCPBSDCompressOption : CCPConfigurationOption(ConfigurationOptionType.BSD_COMPRESS) {
	override fun calculateLength(): Int = 3

	companion object {
		fun read(stream: InputStream): CCPBSDCompressOption {
			stream.read()
			// TODO BSD
			return CCPBSDCompressOption()
		}
	}
}