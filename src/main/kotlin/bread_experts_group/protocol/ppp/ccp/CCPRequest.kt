package bread_experts_group.protocol.ppp.ccp

import bread_experts_group.protocol.ppp.ControlType
import bread_experts_group.protocol.ppp.ccp.option.CompressionControlConfigurationOption
import java.io.InputStream
import java.io.OutputStream

class CCPRequest(
	identifier: Int,
	val options: List<CompressionControlConfigurationOption>
) : CompressionControlProtocolFrame(identifier, ControlType.CONFIGURE_REQUEST) {
	override fun calculateLength(): Int = super.calculateLength() + run {
		this.options.sumOf { it.calculateLength() }
	}

	override fun write(stream: OutputStream) {
		super.write(stream)
		options.forEach { it.write(stream) }
		stream.flush()
	}

	companion object {
		fun read(stream: InputStream, id: Int, length: Int): CCPRequest {
			var remainingLength = length
			val req = CCPRequest(
				id,
				buildList {
					while (remainingLength > 0) {
						val option = CompressionControlConfigurationOption.Companion.read(stream)
						remainingLength -= option.calculateLength()
						add(option)
					}
				}
			)
			return req
		}
	}
}