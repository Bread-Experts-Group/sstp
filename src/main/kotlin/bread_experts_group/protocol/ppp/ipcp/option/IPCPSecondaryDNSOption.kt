package bread_experts_group.protocol.ppp.ipcp.option

import bread_experts_group.util.readInet4
import java.io.InputStream
import java.io.OutputStream
import java.net.Inet4Address

class IPCPSecondaryDNSOption(
	val address: Inet4Address
) : IPCPConfigurationOption(InternetProtocolOptionType.SECONDARY_DNS_SERVER) {
	override fun calculateLength(): Int = 6

	override fun write(stream: OutputStream) {
		super.write(stream)
		stream.write(address.address)
	}

	override fun optionGist(): String = address.toString()

	companion object {
		fun read(stream: InputStream): IPCPSecondaryDNSOption = IPCPSecondaryDNSOption(stream.readInet4())
	}
}