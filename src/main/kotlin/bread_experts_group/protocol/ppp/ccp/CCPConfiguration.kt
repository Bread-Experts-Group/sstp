package bread_experts_group.protocol.ppp.ccp

import bread_experts_group.protocol.ppp.NCPControlType
import bread_experts_group.protocol.ppp.ccp.option.CCPConfigurationOption
import java.io.InputStream
import java.io.OutputStream

sealed class CCPConfiguration(
	identifier: Int,
	val options: List<CCPConfigurationOption>,
	type: NCPControlType
) : CompressionControlProtocolFrame(identifier, type) {
	override fun calculateLength(): Int = super.calculateLength() + run {
		this.options.sumOf { it.calculateLength() }
	}

	override fun write(stream: OutputStream) {
		super.write(stream)
		options.forEach { it.write(stream) }
		stream.flush()
	}

	override fun ccpGist(): String = "# OPT: [${options.size}]" + buildString {
		options.forEach {
			append("\n  ${it.gist()}")
		}
	}

	companion object {
		fun readOpts(stream: InputStream, length: Int): List<CCPConfigurationOption> = buildList {
			var remainingLength = length
			while (remainingLength > 0) {
				val option = CCPConfigurationOption.read(stream)
				remainingLength -= option.calculateLength()
				add(option)
			}
		}
	}
}