package bread_experts_group.protocol.sstp.message

import java.io.OutputStream

open class DataMessage(val data: ByteArray) : Message() {
	override fun calculateLength(): Int = super.calculateLength() + this.data.size

	override fun write(stream: OutputStream) {
		super.write(stream)
		stream.write(this.data)
		stream.flush()
	}
}