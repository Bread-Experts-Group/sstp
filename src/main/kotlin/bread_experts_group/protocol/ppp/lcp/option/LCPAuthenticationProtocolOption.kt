package bread_experts_group.protocol.ppp.lcp.option

import bread_experts_group.util.read16
import bread_experts_group.util.write16
import java.io.InputStream
import java.io.OutputStream

class LCPAuthenticationProtocolOption(
	val protocol: AuthenticationProtocol
) : LCPConfigurationOption(ConfigurationOptionType.AUTHENTICATION_PROTOCOL) {
	enum class AuthenticationProtocol(val code: Int) {
		PASSWORD_AUTHENTICATION_PROTOCOL(0xC023);

		companion object {
			val mapping = entries.associateBy { it.code }
		}
	}

	override fun calculateLength(): Int = 0x4

	override fun write(stream: OutputStream) {
		super.write(stream)
		stream.write16(protocol.code)
	}

	companion object {
		fun read(stream: InputStream): LCPAuthenticationProtocolOption {
			return LCPAuthenticationProtocolOption(AuthenticationProtocol.mapping.getValue(stream.read16()))
		}
	}
}