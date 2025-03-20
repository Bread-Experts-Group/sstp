package bread_experts_group.protocol.ppp.ipv6cp

import bread_experts_group.protocol.ppp.ipv6cp.option.InternetProtocolV6ControlConfigurationOption
import bread_experts_group.protocol.ppp.ControlType
import java.io.InputStream
import java.io.OutputStream

class InternetProtocolV6ControlConfigurationRequest(
	broadcastAddress: Int,
	unnumberedData: Int,
	identifier: Int,
	val options: List<InternetProtocolV6ControlConfigurationOption>
) : InternetProtocolV6ControlProtocolFrame(broadcastAddress, unnumberedData, identifier, ControlType.CONFIGURE_REQUEST) {
	override fun calculateLength(): Int = super.calculateLength() + run {
		this.options.sumOf { it.calculateLength() }
	}

	override fun write(stream: OutputStream) {
		super.write(stream)
		options.forEach { it.write(stream) }
		stream.flush()
	}

	companion object {
		fun read(
			stream: InputStream,
			broadcastAddress: Int, unnumberedData: Int, id: Int, length: Int
		): InternetProtocolV6ControlConfigurationRequest {
			var remainingLength = length
			val req = InternetProtocolV6ControlConfigurationRequest(
				broadcastAddress, unnumberedData, id,
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