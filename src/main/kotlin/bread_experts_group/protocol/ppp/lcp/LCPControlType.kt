package bread_experts_group.protocol.ppp.lcp

enum class LCPControlType(val code: Int) {
	VENDOR_SPECIFIC(0),
	CONFIGURE_REQUEST(1),
	CONFIGURE_ACK(2),
	CONFIGURE_NAK(3),
	CONFIGURE_REJECT(4),
	TERMINATE_REQUEST(5),
	TERMINATE_ACK(6),
	CODE_REJECT(7),
	PROTOCOL_REJECT(8),
	ECHO_REQUEST(9),
	ECHO_REPLY(10),
	DISCARD_REQUEST(11);

	companion object {
		val mapping = entries.associateBy { it.code }
	}
}