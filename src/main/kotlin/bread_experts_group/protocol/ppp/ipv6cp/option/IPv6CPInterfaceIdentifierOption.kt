package bread_experts_group.protocol.ppp.ipv6cp.option

import java.io.InputStream
import java.io.OutputStream

class IPv6CPInterfaceIdentifierOption(
	val identifier: ByteArray
) : IPv6CPConfigurationOption(InternetProtocolV6OptionType.INTERFACE_IDENTIFIER) {
	override fun calculateLength(): Int = 10
	override fun write(stream: OutputStream) {
		super.write(stream)
		stream.write(identifier)
	}

	override fun optionGist(): String = this.identifier.joinToString(":") {
		it.toUByte().toString(16).uppercase().padStart(2, '0')
	}

	companion object {
		fun read(stream: InputStream): IPv6CPInterfaceIdentifierOption = IPv6CPInterfaceIdentifierOption(stream.readNBytes(8))
	}
}