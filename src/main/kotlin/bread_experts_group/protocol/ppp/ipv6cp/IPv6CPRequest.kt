package bread_experts_group.protocol.ppp.ipv6cp

import bread_experts_group.protocol.ppp.NCPControlType
import bread_experts_group.protocol.ppp.ipv6cp.option.IPv6CPConfigurationOption
import java.io.InputStream
import java.io.OutputStream

class IPv6CPRequest(
	identifier: Int,
	val options: List<IPv6CPConfigurationOption>
) : InternetProtocolV6ControlProtocolFrame(identifier, NCPControlType.CONFIGURE_REQUEST) {
	override fun calculateLength(): Int = super.calculateLength() + run {
		this.options.sumOf { it.calculateLength() }
	}

	override fun write(stream: OutputStream) {
		super.write(stream)
		options.forEach { it.write(stream) }
		stream.flush()
	}

	companion object {
		fun read(stream: InputStream, id: Int, length: Int): IPv6CPRequest {
			var remainingLength = length
			val req = IPv6CPRequest(
				id,
				buildList {
					while (remainingLength > 0) {
						val option = IPv6CPConfigurationOption.Companion.read(stream)
						remainingLength -= option.calculateLength()
						add(option)
					}
				}
			)
			return req
		}
	}
}