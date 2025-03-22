package bread_experts_group.protocol.sstp.message

import bread_experts_group.protocol.sstp.attribute.SSTPStatusAttribute

class SSTPAbort(
	status: SSTPStatusAttribute? = null
) : SSTPControlMessage(MessageType.SSTP_MSG_CALL_ABORT, if (status != null) listOf(status) else emptyList())