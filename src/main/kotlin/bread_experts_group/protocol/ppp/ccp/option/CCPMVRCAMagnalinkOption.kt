package bread_experts_group.protocol.ppp.ccp.option

import bread_experts_group.util.read16
import java.io.InputStream

class CCPMVRCAMagnalinkOption : CCPConfigurationOption(ConfigurationOptionType.MVRCA_MAGNALINK) {
	override fun calculateLength(): Int = 4
	override fun optionGist(): String = "MVRCA TODO"

	companion object {
		fun read(stream: InputStream): CCPMVRCAMagnalinkOption {
			stream.read16()
			// TODO Magnalink
			return CCPMVRCAMagnalinkOption()
		}
	}
}