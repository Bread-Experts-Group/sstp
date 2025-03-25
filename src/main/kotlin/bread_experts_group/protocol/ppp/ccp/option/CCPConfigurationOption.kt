package bread_experts_group.protocol.ppp.ccp.option

import bread_experts_group.Writable
import bread_experts_group.util.ToStringUtil.SmartToString
import java.io.InputStream
import java.io.OutputStream

sealed class CCPConfigurationOption(val type: ConfigurationOptionType) : SmartToString(), Writable {
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

	override fun gist(): String = "OPT [${calculateLength()}] $type : ${optionGist()}"
	abstract fun optionGist(): String

	companion object {
		fun read(stream: InputStream): CCPConfigurationOption {
			val type = ConfigurationOptionType.mapping.getValue(stream.read())
			stream.read() // length
			return when (type) {
				ConfigurationOptionType.BSD_COMPRESS -> CCPBSDCompressOption.Companion.read(stream)
				ConfigurationOptionType.MVRCA_MAGNALINK -> CCPMVRCAMagnalinkOption.Companion.read(stream)
				ConfigurationOptionType.DEFLATE -> CCPDEFLATEOption.Companion.read(stream)
			}
		}
	}
}