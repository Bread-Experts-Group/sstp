package bread_experts_group.protocol.dhcp.option

import bread_experts_group.util.readInet4
import bread_experts_group.util.writeInet4
import java.io.InputStream
import java.io.OutputStream
import java.net.Inet4Address

class DHCPSubnetMask(
	val subnetMask: Inet4Address
) : DHCPOption(DHCPOptionType.SUBNET_MASK) {
	override fun optionGist(): String = subnetMask.toString()
	override fun calculateLength(): Int = 4

	override fun write(stream: OutputStream) {
		super.write(stream)
		stream.writeInet4(subnetMask)
	}

	companion object {
		fun read(stream: InputStream): DHCPSubnetMask = DHCPSubnetMask(stream.readInet4())
	}
}