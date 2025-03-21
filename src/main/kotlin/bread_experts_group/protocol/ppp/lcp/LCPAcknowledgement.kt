package bread_experts_group.protocol.ppp.lcp

import bread_experts_group.protocol.ppp.lcp.option.LinkControlConfigurationOption
import java.io.InputStream
import java.io.OutputStream

class LCPAcknowledgement(
	identifier: Int,
	val options: List<LinkControlConfigurationOption>
) : LinkControlProtocolFrame(identifier, LinkControlType.CONFIGURE_ACK) {
	override fun calculateLength(): Int = super.calculateLength() + run {
		this.options.sumOf { it.calculateLength() }
	}

	override fun write(stream: OutputStream) {
		super.write(stream)
		options.forEach { it.write(stream) }
		stream.flush()
	}

	companion object {
		fun read(stream: InputStream, id: Int, length: Int): LCPAcknowledgement {
			var remainingLength = length
			val req = LCPAcknowledgement(
				id,
				buildList {
					while (remainingLength > 0) {
						val option = LinkControlConfigurationOption.Companion.read(stream)
						remainingLength -= option.calculateLength()
						add(option)
					}
				}
			)
			return req
		}
	}
}