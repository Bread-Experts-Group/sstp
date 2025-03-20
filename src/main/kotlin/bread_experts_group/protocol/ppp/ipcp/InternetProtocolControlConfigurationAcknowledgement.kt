package bread_experts_group.protocol.ppp.ipcp

import bread_experts_group.protocol.ppp.ControlType
import bread_experts_group.protocol.ppp.ipcp.option.InternetProtocolControlConfigurationOption
import java.io.InputStream
import java.io.OutputStream

class InternetProtocolControlConfigurationAcknowledgement(
	broadcastAddress: Int,
	unnumberedData: Int,
	identifier: Int,
	val options: List<InternetProtocolControlConfigurationOption>
) : InternetProtocolControlProtocolFrame(broadcastAddress, unnumberedData, identifier, ControlType.CONFIGURE_ACK) {
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
		): InternetProtocolControlConfigurationAcknowledgement {
			var remainingLength = length
			val req = InternetProtocolControlConfigurationAcknowledgement(
				broadcastAddress, unnumberedData, id,
				buildList {
					while (remainingLength > 0) {
						val option = InternetProtocolControlConfigurationOption.Companion.read(stream)
						remainingLength -= option.calculateLength()
						add(option)
					}
				}
			)
			return req
		}
	}
}