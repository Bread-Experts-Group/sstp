package bread_experts_group.protocol.dhcp.option

import bread_experts_group.util.readInet4
import bread_experts_group.util.writeInet4
import java.io.InputStream
import java.io.OutputStream
import java.net.Inet4Address

class DHCPBroadcastAddress(
	val broadcastAddress: Inet4Address
) : DHCPOption(DHCPOptionType.BROADCAST_ADDRESS) {
	override fun optionGist(): String = broadcastAddress.toString()
	override fun calculateLength(): Int = 4

	override fun write(stream: OutputStream) {
		super.write(stream)
		stream.writeInet4(broadcastAddress)
	}

	companion object {
		fun read(stream: InputStream): DHCPBroadcastAddress = DHCPBroadcastAddress(stream.readInet4())
	}
}