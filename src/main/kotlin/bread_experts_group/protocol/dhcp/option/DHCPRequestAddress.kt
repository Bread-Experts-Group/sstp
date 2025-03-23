package bread_experts_group.protocol.dhcp.option

import bread_experts_group.util.readInet4
import java.io.InputStream
import java.io.OutputStream
import java.net.Inet4Address

class DHCPRequestAddress(
	val address: Inet4Address
) : DHCPOption(DHCPOptionType.REQUEST_ADDRESS) {
	override fun optionGist(): String = "$address"
	override fun calculateLength(): Int = 4

	override fun write(stream: OutputStream) {
		super.write(stream)
		stream.write(address.address)
	}

	companion object {
		fun read(stream: InputStream): DHCPRequestAddress = DHCPRequestAddress(stream.readInet4())
	}
}