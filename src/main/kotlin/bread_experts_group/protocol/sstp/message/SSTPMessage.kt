package bread_experts_group.protocol.sstp.message

import bread_experts_group.Writable
import bread_experts_group.util.ToStringUtil.SmartToString
import bread_experts_group.util.hex
import bread_experts_group.util.protocolViolation
import bread_experts_group.util.read16
import bread_experts_group.util.write16
import java.io.InputStream
import java.io.OutputStream

sealed class SSTPMessage : SmartToString(), Writable {
	override fun calculateLength(): Int = 4

	override fun write(stream: OutputStream) {
		stream.write(0x10)
		stream.write(0)
		stream.write16(this.calculateLength())
	}

	override fun gist(): String = "SSTP [${calculateLength()}] "

	companion object {
		fun read(stream: InputStream): SSTPMessage {
			val version = stream.read()
			protocolViolation(version, 0x10, "Version was not 0x10, got ${hex(version)}")
			val control = stream.read() and 1 == 1
			val length = stream.read16()
			if (control) return SSTPControlMessage.read(stream)
			return SSTPDataMessage(stream.readNBytes(length - 4))
		}
	}
}