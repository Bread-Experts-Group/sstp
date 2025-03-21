package bread_experts_group.protocol.ppp.ccp

import bread_experts_group.protocol.ppp.ControlType
import bread_experts_group.protocol.ppp.ccp.option.CompressionControlConfigurationOption
import java.io.InputStream
import java.io.OutputStream

class CCPNonAcknowledgement(
	identifier: Int,
	val options: List<CompressionControlConfigurationOption>
) : CompressionControlProtocolFrame(identifier, ControlType.CONFIGURE_NAK) {
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
		fun read(stream: InputStream, id: Int, length: Int): CCPNonAcknowledgement {
			TODO("CCP")
		}
	}
}