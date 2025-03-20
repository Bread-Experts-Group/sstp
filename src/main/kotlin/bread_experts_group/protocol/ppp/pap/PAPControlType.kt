package bread_experts_group.protocol.ppp.pap

enum class PAPControlType(val code: Int) {
	CONFIGURE_REQUEST(1),
	CONFIGURE_ACK(2),
	CONFIGURE_NAK(3);

	companion object {
		val mapping = entries.associateBy { it.code }
	}
}