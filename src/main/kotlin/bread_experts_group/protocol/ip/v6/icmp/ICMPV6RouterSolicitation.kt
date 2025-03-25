package bread_experts_group.protocol.ip.v6.icmp

import bread_experts_group.Writable
import bread_experts_group.protocol.ip.v6.icmp.option.NDOption
import bread_experts_group.util.read32
import bread_experts_group.util.write32
import java.io.InputStream
import java.io.OutputStream

class ICMPV6RouterSolicitation(
	val options: List<NDOption>
) : ICMPV6Frame(ICMPV6Type.ROUTER_SOLICITATION, 0) {
	override fun calculateLength(): Int = super.calculateLength() + 4
	override fun write(stream: OutputStream, packet: Writable) {
		super.write(stream, packet)
		stream.write32(0)
	}

	override fun icmpGist(): String = "# OPT: [${options.size}]" + buildString {
		options.forEach {
			append("\n  ${it.gist()}")
		}
	}

	companion object {
		fun read(stream: InputStream, length: Int): ICMPV6RouterSolicitation {
			stream.read32() // reserved
			return ICMPV6RouterSolicitation(
				buildList {
					var remainingLength = length - 4
					while (remainingLength > 0) {
						val option = NDOption.read(stream)
						remainingLength -= option.calculateLength()
						add(option)
					}
				}
			)
		}
	}
}