package bread_experts_group.protocol.ppp.ccp.option

import java.io.InputStream
import java.io.OutputStream
import kotlin.math.log
import kotlin.math.pow

class CCPDEFLATEOption(
	val windowSize: Int,
	val method: DEFLATEMethod,
	val checkMethod: DEFLATECheckMethod
) : CCPConfigurationOption(ConfigurationOptionType.DEFLATE) {
	enum class DEFLATEMethod(val code: Int) {
		DEFLATE(0b1000);

		companion object {
			val mapping = entries.associateBy { it.code }
		}
	}

	enum class DEFLATECheckMethod(val code: Int) {
		SEQUENCE(0b00);

		companion object {
			val mapping = entries.associateBy { it.code }
		}
	}

	override fun calculateLength(): Int = 4
	override fun optionGist(): String = "WINDOW_SIZE: $windowSize, METHOD: $method, CHECK_METHOD: $checkMethod"
	override fun write(stream: OutputStream) {
		super.write(stream)
		stream.write(((log(windowSize.toDouble(), 2.0).toInt() - 8) shl 4) or method.code)
		stream.write(checkMethod.code)
	}

	companion object {
		fun read(stream: InputStream): CCPDEFLATEOption {
			val (windowSize, methodRaw) = stream.read().let { 2.0.pow(((it shr 4) + 8).toDouble()) to (it and 0xF) }
			return CCPDEFLATEOption(
				windowSize.toInt(),
				DEFLATEMethod.mapping.getValue(methodRaw),
				DEFLATECheckMethod.mapping.getValue(stream.read() and 0x3)
			)
		}
	}
}