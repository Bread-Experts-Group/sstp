package bread_experts_group.protocol.ppp.ipcp.option.compression

import bread_experts_group.protocol.ppp.ipcp.option.InternetProtocolControlConfigurationOption
import bread_experts_group.util.read16
import bread_experts_group.util.write16
import java.io.InputStream
import java.io.OutputStream

sealed class IPCompressionProtocolOption(
	val protocol: CompressionProtocol
) : InternetProtocolControlConfigurationOption(
	InternetProtocolOptionType.IP_COMPRESSION_PROTOCOL
) {
	enum class CompressionProtocol(val code: Int) {
		VAN_JACOBSON_COMPRESSED_TCP_IP(0x002D);

		companion object {
			val mapping = entries.associateBy { it.code }
		}
	}

	override fun write(stream: OutputStream) {
		super.write(stream)
		stream.write16(this.protocol.code)
	}

	companion object {
		fun read(stream: InputStream): IPCompressionProtocolOption {
			val protocol = CompressionProtocol.mapping.getValue(stream.read16())
			return when (protocol) {
				CompressionProtocol.VAN_JACOBSON_COMPRESSED_TCP_IP -> VanJacobsonCompressedTCPIPOption.read(stream)
			}
		}
	}
}