package bread_experts_group.protocol.ppp.lcp

import bread_experts_group.protocol.ppp.lcp.option.LinkControlConfigurationOption
import java.io.InputStream
import java.io.OutputStream

class LinkControlConfigurationAcknowledgement(
	broadcastAddress: Int,
	unnumberedData: Int,
	identifier: Int,
	val options: List<LinkControlConfigurationOption>
) : LinkControlProtocolFrame(broadcastAddress, unnumberedData, identifier, LinkControlType.CONFIGURE_ACK) {
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
		): LinkControlConfigurationAcknowledgement {
			var remainingLength = length
			val req = LinkControlConfigurationAcknowledgement(
				broadcastAddress, unnumberedData, id,
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