package bread_experts_group.protocol.ppp.ipcp

import bread_experts_group.protocol.ppp.NCPControlType
import bread_experts_group.protocol.ppp.ipcp.option.IPCPConfigurationOption
import java.io.InputStream
import java.io.OutputStream

class IPCPAcknowledgement(
	identifier: Int,
	val options: List<IPCPConfigurationOption>
) : InternetProtocolControlProtocolFrame(identifier, NCPControlType.CONFIGURE_ACK) {
	override fun calculateLength(): Int = super.calculateLength() + run {
		this.options.sumOf { it.calculateLength() }
	}

	override fun write(stream: OutputStream) {
		super.write(stream)
		options.forEach { it.write(stream) }
		stream.flush()
	}

	companion object {
		fun read(stream: InputStream, id: Int, length: Int): IPCPAcknowledgement {
			var remainingLength = length
			val req = IPCPAcknowledgement(
				id,
				buildList {
					while (remainingLength > 0) {
						val option = IPCPConfigurationOption.Companion.read(stream)
						remainingLength -= option.calculateLength()
						add(option)
					}
				}
			)
			return req
		}
	}
}