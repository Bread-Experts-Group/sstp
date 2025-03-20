package bread_experts_group.protocol.ppp.lcp.option

import bread_experts_group.util.read32
import bread_experts_group.util.write32
import java.io.InputStream
import java.io.OutputStream

class MagicNumberOption(
	val number: Int
) : LinkControlConfigurationOption(ConfigurationOptionType.MAGIC_NUMBER) {
	override fun calculateLength(): Int = 0x6

	override fun write(stream: OutputStream) {
		super.write(stream)
		stream.write32(number)
	}

	companion object {
		fun read(stream: InputStream): MagicNumberOption {
			return MagicNumberOption(stream.read32())
		}
	}
}