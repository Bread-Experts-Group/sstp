package bread_experts_group.protocol.sstp.attribute

import bread_experts_group.Writable
import bread_experts_group.util.ToStringUtil.SmartToString
import bread_experts_group.util.read16
import bread_experts_group.util.write16
import java.io.InputStream
import java.io.OutputStream

sealed class SSTPControlMessageAttribute(val type: AttributeType) : SmartToString(), Writable {
	enum class AttributeType(val code: Int) {
		SSTP_ATTRIB_ENCAPSULATED_PROTOCOL_ID(0x01),
		SSTP_ATTRIB_STATUS_INFO(0x02),
		SSTP_ATTRIB_CRYPTO_BINDING(0x03),
		SSTP_ATTRIB_CRYPTO_BINDING_REQ(0x04);

		companion object {
			val mapping = entries.associateBy { it.code }
		}
	}

	override fun calculateLength(): Int = 4

	override fun write(stream: OutputStream) {
		stream.write(0)
		stream.write(this.type.code)
		stream.write16(this.calculateLength())
	}

	final override fun gist(): String = "ATTRIB [${calculateLength()}] $type : ${attribGist()}"
	abstract fun attribGist(): String

	companion object {
		fun read(stream: InputStream): SSTPControlMessageAttribute {
			stream.read()
			val id = AttributeType.mapping.getValue(stream.read())
			stream.read16()
			return when (id) {
				AttributeType.SSTP_ATTRIB_ENCAPSULATED_PROTOCOL_ID -> SSTPEncapsulatedProtocolAttribute.read(stream)
				AttributeType.SSTP_ATTRIB_CRYPTO_BINDING_REQ -> SSTPCryptoBindingRequestAttribute.read(stream)
				AttributeType.SSTP_ATTRIB_CRYPTO_BINDING -> SSTPCryptoBindingAttribute.read(stream)
				AttributeType.SSTP_ATTRIB_STATUS_INFO -> SSTPStatusAttribute.read(stream)
			}
		}
	}
}