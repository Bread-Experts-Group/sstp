package bread_experts_group.protocol.dhcp.option

import bread_experts_group.util.readInet4
import bread_experts_group.util.writeInet4
import java.io.InputStream
import java.io.OutputStream
import java.net.Inet4Address

class DHCPRouters(
	val routers: List<Inet4Address>
) : DHCPOption(DHCPOptionType.ROUTER) {
	override fun optionGist(): String = "# ROUTERS: [${routers.size}]" + buildString {
		routers.forEach {
			append("\n    $it")
		}
	}

	override fun calculateLength(): Int = 2 + (routers.size * 4)

	override fun write(stream: OutputStream) {
		super.write(stream)
		routers.forEach { stream.writeInet4(it) }
	}

	companion object {
		fun read(stream: InputStream, length: Int): DHCPRouters = DHCPRouters(
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