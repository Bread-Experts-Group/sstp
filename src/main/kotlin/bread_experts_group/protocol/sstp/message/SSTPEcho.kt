package bread_experts_group.protocol.sstp.message

import java.io.OutputStream

class SSTPEcho(request: Boolean) : SSTPControlMessage(
	if (request) MessageType.SSTP_MSG_ECHO_REQUEST else MessageType.SSTP_MSG_ECHO_RESPONSE,
	emptyList()
) {
	override fun write(stream: OutputStream) {
		super.write(stream)
		stream.flush()
	}
}