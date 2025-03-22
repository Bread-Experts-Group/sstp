package bread_experts_group.protocol.sstp.attribute

import bread_experts_group.util.read16
import bread_experts_group.util.write16
import java.io.InputStream
import java.io.OutputStream

class SSTPEncapsulatedProtocolAttribute(
	val protocolID: ProtocolType
) : SSTPControlMessageAttribute(AttributeType.SSTP_ATTRIB_ENCAPSULATED_PROTOCOL_ID) {
	enum class ProtocolType(val code: Int) {
		SSTP_ENCAPSULATED_PROTOCOL_PPP(0x0001);

		companion object {
			val mapping = entries.associateBy { it.code }
		}
	}

	override fun calculateLength(): Int = 0x006

	override fun write(stream: OutputStream) {
		super.write(stream)
		stream.write16(this.protocolID.code)
		stream.flush()
	}

	override fun attribGist(): String = protocolID.name

	companion object {
		fun read(stream: InputStream): SSTPEncapsulatedProtocolAttribute {
			return SSTPEncapsulatedProtocolAttribute(ProtocolType.mapping.getValue(stream.read16()))
		}
	}
}