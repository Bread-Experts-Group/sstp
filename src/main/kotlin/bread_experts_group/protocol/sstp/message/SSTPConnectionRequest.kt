package bread_experts_group.protocol.sstp.message

import bread_experts_group.protocol.sstp.attribute.SSTPControlMessageAttribute
import bread_experts_group.protocol.sstp.attribute.SSTPEncapsulatedProtocolAttribute

class SSTPConnectionRequest(
	val attribute: SSTPEncapsulatedProtocolAttribute
) : SSTPControlMessage(MessageType.SSTP_MSG_CALL_CONNECT_REQUEST, listOf(attribute)) {
	constructor(attributes: List<SSTPControlMessageAttribute>) : this(attributes.first() as SSTPEncapsulatedProtocolAttribute)

	override fun calculateLength(): Int = 0x00E
}