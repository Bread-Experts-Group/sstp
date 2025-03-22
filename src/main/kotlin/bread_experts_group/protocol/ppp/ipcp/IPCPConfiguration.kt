package bread_experts_group.protocol.ppp.ipcp

import bread_experts_group.protocol.ppp.NCPControlType
import bread_experts_group.protocol.ppp.ipcp.option.IPCPConfigurationOption
import java.io.InputStream
import java.io.OutputStream

sealed class IPCPConfiguration(
	identifier: Int,
	val options: List<IPCPConfigurationOption>,
	type: NCPControlType
) : InternetProtocolControlProtocolFrame(identifier, type) {
	override fun calculateLength(): Int = super.calculateLength() + run {
		this.options.sumOf { it.calculateLength() }
	}

	override fun write(stream: OutputStream) {
		super.write(stream)
		options.forEach { it.write(stream) }
		stream.flush()
	}

	override fun ipcpGist(): String = "# OPT: [${options.size}]" + buildString {
		options.forEach {
			append("\n  ${it.gist()}")
		}
	}

	companion object {
		fun readOpts(stream: InputStream, length: Int): List<IPCPConfigurationOption> = buildList {
			var remainingLength = length
			while (remainingLength > 0) {
				val option = IPCPConfigurationOption.read(stream)
				remainingLength -= option.calculateLength()
				add(option)
			}
		}
	}
}