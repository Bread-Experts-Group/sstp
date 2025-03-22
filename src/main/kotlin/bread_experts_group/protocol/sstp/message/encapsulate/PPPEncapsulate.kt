package bread_experts_group.protocol.sstp.message.encapsulate

import bread_experts_group.protocol.ppp.PointToPointProtocolFrame
import bread_experts_group.protocol.sstp.message.SSTPDataMessage
import java.io.ByteArrayOutputStream
import java.io.OutputStream

open class PPPEncapsulate<T : PointToPointProtocolFrame>(val pppFrame: T) : SSTPDataMessage(
	ByteArrayOutputStream().use { pppFrame.write(it); it.toByteArray() }
) {
	override fun write(stream: OutputStream) {
		super.write(stream)
		stream.flush()
	}

	override fun gist(): String = "${super.gist()}\n${pppFrame.gist()}"
}