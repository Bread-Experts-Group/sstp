package bread_experts_group.protocol.ppp.lcp.option

import bread_experts_group.util.read32
import bread_experts_group.util.write32
import java.io.InputStream
import java.io.OutputStream

class AsynchronousControlCharacterMapOption(
	val map: Map<Int, Boolean>
) : LinkControlConfigurationOption(ConfigurationOptionType.ASYNCHRONOUS_CONTROL_CHARACTER_MAP) {
	override fun calculateLength(): Int = 0x6

	override fun write(stream: OutputStream) {
		super.write(stream)
		var bits = 0
		this.map.forEach { if (it.value) bits = bits or (1 shl it.key) }
		stream.write32(bits)
	}

	companion object {
		fun read(stream: InputStream): AsynchronousControlCharacterMapOption {
			val map = stream.read32()
			return AsynchronousControlCharacterMapOption(
				buildMap {
					for (i in 0..31) set(i, (map and (1 shl i)) > 0)
				}
			)
		}
	}
}