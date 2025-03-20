package bread_experts_group.protocol.ppp.ccp.option

import bread_experts_group.util.read16
import java.io.InputStream

class DEFLATEOption : CompressionControlConfigurationOption(ConfigurationOptionType.DEFLATE) {
	override fun calculateLength(): Int = 4

	companion object {
		fun read(stream: InputStream): DEFLATEOption {
			val data = stream.read16()
			// TODO Deflate
			return DEFLATEOption()
		}
	}
}