package bread_experts_group.protocol.sstp.attribute

import bread_experts_group.util.read32
import bread_experts_group.util.write24
import bread_experts_group.util.write32
import java.io.InputStream
import java.io.OutputStream

class SSTPStatusAttribute(
	val attributeID: AttributeTypeStatus,
	val status: Status,
	val attribute: SSTPControlMessageAttribute? = null
) : SSTPControlMessageAttribute(AttributeType.SSTP_ATTRIB_STATUS_INFO) {
	enum class AttributeTypeStatus(val code: Int) {
		SSTP_ATTRIB_NO_ERROR(0x00),
		SSTP_ATTRIB_ENCAPSULATED_PROTOCOL_ID(0x01),
		SSTP_ATTRIB_STATUS_INFO(0x02),
		SSTP_ATTRIB_CRYPTO_BINDING(0x03),
		SSTP_ATTRIB_CRYPTO_BINDING_REQ(0x04);

		companion object {
			val mapping = entries.associateBy { it.code }
		}
	}

	enum class Status(val code: Int) {
		ATTRIB_STATUS_NO_ERROR(0x00000000),
		ATTRIB_STATUS_DUPLICATE_ATTRIBUTE(0x00000001),
		ATTRIB_STATUS_UNRECOGNIZED_ATTRIBUTE(0x00000002),
		ATTRIB_STATUS_INVALID_ATTRIB_VALUE_LENGTH(0x00000003),
		ATTRIB_STATUS_VALUE_NOT_SUPPORTED(0x00000004),
		ATTRIB_STATUS_UNACCEPTED_FRAME_RECEIVED(0x00000005),
		ATTRIB_STATUS_RETRY_COUNT_EXCEEDED(0x00000006),
		ATTRIB_STATUS_INVALID_FRAME_RECEIVED(0x00000007),
		ATTRIB_STATUS_NEGOTIATION_TIMEOUT(0x00000008),
		ATTRIB_STATUS_ATTRIB_NOT_SUPPORTED_IN_MSG(0x00000009),
		ATTRIB_STATUS_REQUIRED_ATTRIBUTE_MISSING(0x0000000A),
		ATTRIB_STATUS_STATUS_INFO_NOT_SUPPORTED_IN_MSG(0x0000000B);

		companion object {
			val mapping = entries.associateBy { it.code }
		}
	}

	override fun calculateLength(): Int = super.calculateLength() + 8 + (attribute?.calculateLength() ?: 0)

	override fun write(stream: OutputStream) {
		super.write(stream)
		stream.write24(0)
		stream.write(attributeID.code)
		stream.write32(status.code)
		attribute?.write(stream)
		stream.flush()
	}

	override fun attribGist(): String = "ATTRIBUTE: $attributeID, STATUS: $status, " +
			"# ATRB DATA: ${attribute?.calculateLength() ?: 0}"

	companion object {
		fun read(stream: InputStream): SSTPStatusAttribute {
			return SSTPStatusAttribute(
				stream.skip(3).let { AttributeTypeStatus.mapping.getValue(stream.read()) },
				Status.mapping.getValue(stream.read32()),
				SSTPControlMessageAttribute.read(stream)
			)
		}
	}
}