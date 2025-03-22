package bread_experts_group.protocol.sstp.message

import bread_experts_group.protocol.sstp.attribute.SSTPControlMessageAttribute
import bread_experts_group.protocol.sstp.attribute.SSTPCryptoBindingAttribute

// TODO verification
@Suppress("CanBeParameter")
class SSTPConnected(
	val attribute: SSTPCryptoBindingAttribute
) : SSTPControlMessage(MessageType.SSTP_MSG_CALL_CONNECT_REQUEST, listOf(attribute)) {
	constructor(attributes: List<SSTPControlMessageAttribute>) : this(attributes.first() as SSTPCryptoBindingAttribute)

	override fun calculateLength(): Int = 0x070
}