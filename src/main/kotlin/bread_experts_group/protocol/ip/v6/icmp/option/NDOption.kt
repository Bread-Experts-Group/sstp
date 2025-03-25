package bread_experts_group.protocol.ip.v6.icmp.option

import bread_experts_group.Writable
import bread_experts_group.util.ToStringUtil.SmartToString
import java.io.InputStream
import java.io.OutputStream

sealed class NDOption(val type: NDOptionType) : SmartToString(), Writable {
	enum class NDOptionType(val code: Int) {
		;

		companion object {
			val mapping = entries.associateBy { it.code }
		}
	}

	override fun calculateLength(): Int = 2
	override fun write(stream: OutputStream) {
		stream.write(type.code)
		stream.write(calculateLength())
	}

	final override fun gist(): String = "OPT [${calculateLength()}] $type : ${optionGist()}"
	abstract fun optionGist(): String

	companion object {
		fun read(stream: InputStream): NDOption {
			val type = NDOptionType.mapping.getValue(stream.read())
			return when (type) {
				else -> TODO(type.name)
			}
		}
	}
}