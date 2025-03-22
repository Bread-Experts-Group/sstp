package bread_experts_group.protocol.ppp.ccp

import bread_experts_group.protocol.ppp.NCPControlType
import bread_experts_group.protocol.ppp.ccp.option.CCPConfigurationOption
import java.io.InputStream
import java.io.OutputStream

class CCPRequest(
	identifier: Int,
	val options: List<CCPConfigurationOption>
) : CompressionControlProtocolFrame(identifier, NCPControlType.CONFIGURE_REQUEST) {
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
						val option = CCPConfigurationOption.Companion.read(stream)
						remainingLength -= option.calculateLength()
						add(option)
					}
				}
			)
			return req
		}
	}
}