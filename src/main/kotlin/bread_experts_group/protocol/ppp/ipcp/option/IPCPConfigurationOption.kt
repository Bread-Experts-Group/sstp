package bread_experts_group.protocol.ppp.ipcp.option

import bread_experts_group.Writable
import bread_experts_group.protocol.ppp.ipcp.option.compression.IPCPCompressionOption
import bread_experts_group.util.ToStringUtil.SmartToString
import java.io.InputStream
import java.io.OutputStream

abstract class IPCPConfigurationOption(
	val type: InternetProtocolOptionType
) : SmartToString(), Writable {
	enum class InternetProtocolOptionType(val code: Int) {
		IP_COMPRESSION_PROTOCOL(2),
		IP_ADDRESS(3);

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
		fun read(stream: InputStream): IPCPConfigurationOption {
			val type = InternetProtocolOptionType.mapping.getValue(stream.read())
			stream.read() // length
			return when (type) {
				InternetProtocolOptionType.IP_COMPRESSION_PROTOCOL -> IPCPCompressionOption.read(stream)
				InternetProtocolOptionType.IP_ADDRESS -> IPCPAddressOption.read(stream)
			}
		}
	}
}