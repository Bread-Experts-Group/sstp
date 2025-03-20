package bread_experts_group.protocol.ppp.ccp.option

import bread_experts_group.Writable
import bread_experts_group.util.ToStringUtil
import java.io.InputStream
import java.io.OutputStream

sealed class CompressionControlConfigurationOption(val type: ConfigurationOptionType) : ToStringUtil.SmartToString(), Writable {
	enum class ConfigurationOptionType(val code: Int) {
		BSD_COMPRESS(21),
		MVRCA_MAGNALINK(24),
		DEFLATE(26);

		companion object {
			val mapping = entries.associateBy { it.code }
		}
	}

	override fun write(stream: OutputStream) {
		stream.write(this.type.code)
		stream.write(this.calculateLength())
	}

	companion object {
		fun read(stream: InputStream): CompressionControlConfigurationOption {
			val type = ConfigurationOptionType.mapping.getValue(stream.read())
			stream.read() // length
			return when (type) {
				ConfigurationOptionType.BSD_COMPRESS -> BSDCompressOption.Companion.read(stream)
				ConfigurationOptionType.MVRCA_MAGNALINK -> MVRCAMagnalinkOption.Companion.read(stream)
				ConfigurationOptionType.DEFLATE -> DEFLATEOption.Companion.read(stream)
				else -> TODO(type.toString())
			}
		}
	}
}