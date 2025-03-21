package bread_experts_group.protocol.sstp.message

import bread_experts_group.protocol.ppp.PPPFrame
import java.io.ByteArrayOutputStream
import java.io.OutputStream

open class PPPEncapsulate<T : PPPFrame>(val pppFrame: T) : DataMessage(
	ByteArrayOutputStream().use { pppFrame.write(it); it.toByteArray() }
) {
	override fun write(stream: OutputStream) {
		super.write(stream)
		stream.flush()
	}
}