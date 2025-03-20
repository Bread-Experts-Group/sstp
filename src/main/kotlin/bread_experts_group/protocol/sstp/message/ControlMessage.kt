package bread_experts_group.protocol.sstp.message

import bread_experts_group.protocol.sstp.attribute.ControlMessageAttribute
import bread_experts_group.util.read16
import bread_experts_group.util.write16
import java.io.InputStream
import java.io.OutputStream

sealed class ControlMessage(val type: MessageType, val attributes: List<ControlMessageAttribute>) : Message() {
	enum class MessageType(val code: Int) {
		SSTP_MSG_CALL_CONNECT_REQUEST(0x0001),
		SSTP_MSG_CALL_CONNECT_ACK(0x0002),
		SSTP_MSG_CALL_CONNECT_NAK(0x0003),
		SSTP_MSG_CALL_CONNECTED(0x0004),
		SSTP_MSG_CALL_ABORT(0x0005),
		SSTP_MSG_CALL_DISCONNECT(0x0006),
		SSTP_MSG_CALL_DISCONNECT_ACK(0x0007),
		SSTP_MSG_ECHO_REQUEST(0x0008),
		SSTP_MSG_ECHO_RESPONSE(0x0009);

		companion object {
			val mapping = entries.associateBy { it.code }
		}
	}

	override fun calculateLength(): Int = super.calculateLength() + run {
		4 + this.attributes.sumOf { it.calculateLength() }
	}

	override fun write(stream: OutputStream) {
		stream.write(0x10)
		stream.write(1)
		stream.write16(this.calculateLength())
		stream.write16(this.type.code)
		stream.write16(this.attributes.size)
		this.attributes.forEach {
			it.write(stream)
		}
	}

	companion object {
		fun read(stream: InputStream): ControlMessage {
			val type = MessageType.mapping.getValue(stream.read16())
			val attributes = List(stream.read16()) { ControlMessageAttribute.read(stream) }
			return when (type) {
				MessageType.SSTP_MSG_CALL_CONNECT_REQUEST -> ConnectionRequest(attributes)
				MessageType.SSTP_MSG_CALL_CONNECTED -> Connected(attributes)
				else -> throw IllegalArgumentException("Unknown type $type, $attributes")
			}
		}
	}
}