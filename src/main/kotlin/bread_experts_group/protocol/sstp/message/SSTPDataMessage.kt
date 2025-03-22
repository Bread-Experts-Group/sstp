package bread_experts_group.protocol.sstp.message

import java.io.OutputStream

open class SSTPDataMessage(val data: ByteArray) : SSTPMessage() {
	override fun calculateLength(): Int = super.calculateLength() + this.data.size

	override fun write(stream: OutputStream) {
		super.write(stream)
		stream.write(this.data)
		stream.flush()
	}

	override fun gist(): String = super.gist() + "DATA [${data.size}]"
}