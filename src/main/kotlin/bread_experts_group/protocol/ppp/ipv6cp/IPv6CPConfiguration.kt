package bread_experts_group.protocol.ppp.ipv6cp

import bread_experts_group.protocol.ppp.NCPControlType
import bread_experts_group.protocol.ppp.ipv6cp.option.IPv6CPConfigurationOption
import java.io.InputStream
import java.io.OutputStream

sealed class IPv6CPConfiguration(
	identifier: Int,
	val options: List<IPv6CPConfigurationOption>,
	type: NCPControlType
) : InternetProtocolV6ControlProtocolFrame(identifier, type) {
	override fun calculateLength(): Int = super.calculateLength() + run {
		this.options.sumOf { it.calculateLength() }
	}

	override fun write(stream: OutputStream) {
		super.write(stream)
		options.forEach { it.write(stream) }
		stream.flush()
	}

	override fun ipv6cpGist(): String = "# OPT: [${options.size}]" + buildString {
		options.forEach {
			append("\n  ${it.gist()}")
		}
	}

	companion object {
		fun readOpts(stream: InputStream, length: Int): List<IPv6CPConfigurationOption> = buildList {
			var remainingLength = length
			while (remainingLength > 0) {
				val option = IPv6CPConfigurationOption.read(stream)
				remainingLength -= option.calculateLength()
				add(option)
			}
		}
	}
}