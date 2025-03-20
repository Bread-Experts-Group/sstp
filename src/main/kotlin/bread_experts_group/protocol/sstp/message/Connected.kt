package bread_experts_group.protocol.sstp.message

import bread_experts_group.protocol.sstp.attribute.ControlMessageAttribute
import bread_experts_group.protocol.sstp.attribute.CryptoBindingAttribute

class Connected(
	val attribute: CryptoBindingAttribute
) : ControlMessage(MessageType.SSTP_MSG_CALL_CONNECT_REQUEST, listOf(attribute)) {
	constructor(attributes: List<ControlMessageAttribute>) : this(attributes.first() as CryptoBindingAttribute)

	override fun calculateLength(): Int = 0x070
}