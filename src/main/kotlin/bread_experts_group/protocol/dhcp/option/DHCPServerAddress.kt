package bread_experts_group.protocol.dhcp.option

import bread_experts_group.util.readInet4
import java.io.InputStream
import java.io.OutputStream
import java.net.Inet4Address

class DHCPServerAddress(
	val address: Inet4Address
) : DHCPOption(DHCPOptionType.SERVER_ADDRESS) {
	override fun optionGist(): String = "IP: $address"
	override fun calculateLength(): Int = 6

	override fun write(stream: OutputStream) {
		super.write(stream)
		stream.write(address.address)
	}

	companion object {
		fun read(stream: InputStream): DHCPServerAddress = DHCPServerAddress(stream.readInet4())
	}
}