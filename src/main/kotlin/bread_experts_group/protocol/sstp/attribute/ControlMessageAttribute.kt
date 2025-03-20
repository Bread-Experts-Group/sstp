package bread_experts_group.protocol.sstp.attribute

import bread_experts_group.Writable
import bread_experts_group.util.ToStringUtil.SmartToString
import bread_experts_group.util.read16
import bread_experts_group.util.write16
import java.io.InputStream
import java.io.OutputStream

sealed class ControlMessageAttribute(val type: AttributeType) : SmartToString(), Writable {
	enum class AttributeType(val code: Int) {
		SSTP_ATTRIB_ENCAPSULATED_PROTOCOL_ID(0x01),
		SSTP_ATTRIB_STATUS_INFO(0x02),
		SSTP_ATTRIB_CRYPTO_BINDING(0x03),
		SSTP_ATTRIB_CRYPTO_BINDING_REQ(0x04);

		companion object {
			val mapping = entries.associateBy { it.code }
		}
	}

	override fun write(stream: OutputStream) {
		stream.write(0)
		stream.write(this.type.code)
		stream.write16(this.calculateLength())
	}

	companion object {
		fun read(stream: InputStream): ControlMessageAttribute {
			stream.read()
			val id = AttributeType.mapping.getValue(stream.read())
			stream.read16()
			return when (id) {
				AttributeType.SSTP_ATTRIB_ENCAPSULATED_PROTOCOL_ID -> EncapsulatedProtocolAttribute.read(stream)
				AttributeType.SSTP_ATTRIB_CRYPTO_BINDING_REQ -> CryptoBindingRequestAttribute.read(stream)
				AttributeType.SSTP_ATTRIB_CRYPTO_BINDING -> CryptoBindingAttribute.read(stream)
				else -> throw IllegalArgumentException("Unknown attribute type $id")
			}
		}
	}
}