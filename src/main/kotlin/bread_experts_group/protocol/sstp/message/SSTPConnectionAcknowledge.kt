package bread_experts_group.protocol.sstp.message

import bread_experts_group.protocol.sstp.attribute.SSTPCryptoBindingRequestAttribute

class SSTPConnectionAcknowledge(
	attribute: SSTPCryptoBindingRequestAttribute
) : SSTPControlMessage(MessageType.SSTP_MSG_CALL_CONNECT_ACK, listOf(attribute)) {
	override fun calculateLength(): Int = 0x030
}