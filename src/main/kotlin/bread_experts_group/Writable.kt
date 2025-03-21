package bread_experts_group

import java.io.ByteArrayOutputStream
import java.io.OutputStream

interface Writable {
	fun calculateLength(): Int
	fun write(stream: OutputStream)
	fun asBytes(): ByteArray = ByteArrayOutputStream().also(::write).toByteArray()
}