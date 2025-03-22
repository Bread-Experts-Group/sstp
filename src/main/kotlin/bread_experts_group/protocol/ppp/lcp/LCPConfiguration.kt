package bread_experts_group.protocol.ppp.lcp

import bread_experts_group.protocol.ppp.lcp.option.LCPConfigurationOption
import java.io.InputStream
import java.io.OutputStream

sealed class LCPConfiguration(
	identifier: Int,
	val options: List<LCPConfigurationOption>,
	type: LCPControlType
) : LinkControlProtocolFrame(identifier, type) {
	override fun calculateLength(): Int = super.calculateLength() + run {
		this.options.sumOf { it.calculateLength() }
	}

	override fun write(stream: OutputStream) {
		super.write(stream)
		options.forEach { it.write(stream) }
		stream.flush()
	}

	override fun lcpGist(): String = "# OPT: [${options.size}]" + buildString {
		options.forEach {
			append("\n  ${it.gist()}")
		}
	}

	companion object {
		fun readOpts(stream: InputStream, length: Int): List<LCPConfigurationOption> = buildList {
			var remainingLength = length
			while (remainingLength > 0) {
				val option = LCPConfigurationOption.Companion.read(stream)
				remainingLength -= option.calculateLength()
				add(option)
			}
		}
	}
}