package bread_experts_group.protocol.sstp.message

import bread_experts_group.protocol.sstp.attribute.CryptoBindingRequestAttribute

class ConnectionAcknowledge(
	attribute: CryptoBindingRequestAttribute
) : ControlMessage(MessageType.SSTP_MSG_CALL_CONNECT_ACK, listOf(attribute)) {
	override fun calculateLength(): Int = 0x030
}