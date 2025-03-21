package bread_experts_group.protocol.ppp.ipv6cp

import bread_experts_group.protocol.ppp.ControlType
import bread_experts_group.protocol.ppp.ipv6cp.option.InternetProtocolV6ControlConfigurationOption
import java.io.InputStream
import java.io.OutputStream

class IPv6CPRequest(
	identifier: Int,
	val options: List<InternetProtocolV6ControlConfigurationOption>
) : InternetProtocolV6ControlProtocolFrame(identifier, ControlType.CONFIGURE_REQUEST) {
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
						val option = InternetProtocolV6ControlConfigurationOption.Companion.read(stream)
						remainingLength -= option.calculateLength()
						add(option)
					}
				}
			)
			return req
		}
	}
}