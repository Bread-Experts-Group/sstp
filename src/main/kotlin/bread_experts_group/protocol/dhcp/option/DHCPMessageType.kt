package bread_experts_group.protocol.dhcp.option

import java.io.InputStream
import java.io.OutputStream

class DHCPMessageType(
	val messageType: DHCPMessageTypes
) : DHCPOption(DHCPOptionType.MESSAGE_TYPE) {
	enum class DHCPMessageTypes(val code: Int) {
		DHCPDISCOVER(1),
		DHCPOFFER(2),
		DHCPREQUEST(3),
		DHCPDECLINE(4),
		DHCPACK(5),
		DHCPNAK(6),
		DHCPRELEASE(7),
		DHCPINFORM(8);

		companion object {
			val mapping = entries.associateBy { it.code }
		}
	}

	override fun optionGist(): String = messageType.name
	override fun calculateLength(): Int = 1

	override fun write(stream: OutputStream) {
		super.write(stream)
		stream.write(messageType.code)
	}

	companion object {
		fun read(stream: InputStream): DHCPMessageType = DHCPMessageType(
			DHCPMessageTypes.mapping.getValue(stream.read())
		)
	}
}