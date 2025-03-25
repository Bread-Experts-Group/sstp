package bread_experts_group.protocol.ppp.ipv6cp.option

import bread_experts_group.Writable
import bread_experts_group.util.ToStringUtil.SmartToString
import java.io.InputStream
import java.io.OutputStream

abstract class IPv6CPConfigurationOption(
	val type: InternetProtocolV6OptionType
) : SmartToString(), Writable {
	enum class InternetProtocolV6OptionType(val code: Int) {
		INTERFACE_IDENTIFIER(1);

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
		fun read(stream: InputStream): IPv6CPConfigurationOption {
			val type = InternetProtocolV6OptionType.mapping.getValue(stream.read())
			stream.read() // length
			return when (type) {
				InternetProtocolV6OptionType.INTERFACE_IDENTIFIER -> IPv6CPInterfaceIdentifierOption.read(stream)
			}
		}
	}
}