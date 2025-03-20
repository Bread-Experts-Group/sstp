package bread_experts_group.protocol.ppp.ipcp

import bread_experts_group.protocol.ppp.ControlType
import bread_experts_group.protocol.ppp.ipcp.option.InternetProtocolControlConfigurationOption
import java.io.OutputStream

class InternetProtocolControlConfigurationNonAcknowledgement(
	broadcastAddress: Int,
	unnumberedData: Int,
	identifier: Int,
	val options: List<InternetProtocolControlConfigurationOption>
) : InternetProtocolControlProtocolFrame(broadcastAddress, unnumberedData, identifier, ControlType.CONFIGURE_NAK) {
	override fun calculateLength(): Int = super.calculateLength() + run {
		this.options.sumOf { it.calculateLength() }
	}

	override fun write(stream: OutputStream) {
		super.write(stream)
		options.forEach { it.write(stream) }
		stream.flush()
	}
}