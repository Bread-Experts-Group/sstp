package bread_experts_group.protocol.ppp.lcp

import bread_experts_group.protocol.ppp.lcp.option.LCPConfigurationOption
import java.io.InputStream
import java.io.OutputStream

class LCPNonAcknowledgement(
	identifier: Int,
	val options: List<LCPConfigurationOption>
) : LinkControlProtocolFrame(identifier, LCPControlType.CONFIGURE_NAK) {
	override fun calculateLength(): Int = super.calculateLength() + run {
		this.options.sumOf { it.calculateLength() }
	}

	override fun write(stream: OutputStream) {
		super.write(stream)
		options.forEach { it.write(stream) }
		stream.flush()
	}

	companion object {
		@Suppress("unused")
		fun read(stream: InputStream, id: Int, length: Int): LCPNonAcknowledgement {
			TODO("LCP NonAck")
		}
	}
}