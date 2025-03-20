package bread_experts_group.protocol.ppp.ccp.option

import java.io.InputStream

class BSDCompressOption : CompressionControlConfigurationOption(ConfigurationOptionType.BSD_COMPRESS) {
	override fun calculateLength(): Int = 3

	companion object {
		fun read(stream: InputStream): BSDCompressOption {
			val data = stream.read()
			// TODO BSD
			return BSDCompressOption()
		}
	}
}