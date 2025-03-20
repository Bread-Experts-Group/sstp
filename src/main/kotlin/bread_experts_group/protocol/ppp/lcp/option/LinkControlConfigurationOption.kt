package bread_experts_group.protocol.ppp.lcp.option

import bread_experts_group.Writable
import bread_experts_group.util.ToStringUtil
import java.io.InputStream
import java.io.OutputStream

sealed class LinkControlConfigurationOption(val type: ConfigurationOptionType) : ToStringUtil.SmartToString(), Writable {
	enum class ConfigurationOptionType(val code: Int) {
		MAXIMUM_RECEIVE_UNIT(1),
		ASYNCHRONOUS_CONTROL_CHARACTER_MAP(2),
		AUTHENTICATION_PROTOCOL(3),
		QUALITY_PROTOCOL(4),
		MAGIC_NUMBER(5),
		PROTOCOL_FIELD_COMPRESSION(7),
		ADDRESS_AND_CONTROL_FIELD_COMPRESSION(8);

		companion object {
			val mapping = entries.associateBy { it.code }
		}
	}

	override fun write(stream: OutputStream) {
		stream.write(this.type.code)
		stream.write(this.calculateLength())
	}

	companion object {
		fun read(stream: InputStream): LinkControlConfigurationOption {
			val type = ConfigurationOptionType.mapping.getValue(stream.read())
			stream.read() // length
			return when (type) {
				ConfigurationOptionType.ASYNCHRONOUS_CONTROL_CHARACTER_MAP ->
					AsynchronousControlCharacterMapOption.read(stream)

				ConfigurationOptionType.MAGIC_NUMBER ->
					MagicNumberOption.read(stream)

				ConfigurationOptionType.PROTOCOL_FIELD_COMPRESSION ->
					ProtocolFieldCompressionOption.read(stream)

				ConfigurationOptionType.ADDRESS_AND_CONTROL_FIELD_COMPRESSION ->
					AddressAndControlCompressionOption.read(stream)

				ConfigurationOptionType.AUTHENTICATION_PROTOCOL ->
					AuthenticationProtocolOption.read(stream)

				else -> TODO(type.toString())
			}
		}
	}
}