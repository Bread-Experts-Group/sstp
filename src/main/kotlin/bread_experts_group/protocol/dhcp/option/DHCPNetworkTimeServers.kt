package bread_experts_group.protocol.dhcp.option

import bread_experts_group.util.readInet4
import bread_experts_group.util.writeInet4
import java.io.InputStream
import java.io.OutputStream
import java.net.Inet4Address

class DHCPNetworkTimeServers(
	val servers: List<Inet4Address>
) : DHCPOption(DHCPOptionType.NETWORK_TIME_SERVERS) {
	override fun optionGist(): String = "# SERVERS: [${servers.size}]" + buildString {
		servers.forEach {
			append("\n   $it")
		}
	}

	override fun calculateLength(): Int = servers.size * 4

	override fun write(stream: OutputStream) {
		super.write(stream)
		servers.forEach { stream.writeInet4(it) }
	}

	companion object {
		fun read(stream: InputStream, length: Int): DHCPNetworkTimeServers = DHCPNetworkTimeServers(
			buildList {
				var remainingLength = length
				while (remainingLength > 0) {
					remainingLength -= 4
					add(stream.readInet4())
				}
			}
		)
	}
}