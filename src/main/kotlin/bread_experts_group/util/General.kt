package bread_experts_group.util

import java.io.InputStream
import java.io.OutputStream
import java.lang.String.format
import java.net.Inet4Address
import java.util.*

fun hex(value: Int): String = "0x${value.toString(16).uppercase()}"

const val ESC = "\u001b["
const val RST = ESC + "0m"
fun log(color: Int? = WHITE, text: Any?, vararg arg: Any?) {
	System.out.printf(
		"%s[%s @ %tT] $text$RST",
		"${ESC}38;5;${color}m",
		Thread.currentThread().name.padEnd(28),
		Calendar.getInstance(),
		*arg
	)
}

const val GRAY = 7
const val PALE_BLUE = 105
const val BLUE = 27
const val PALE_ORANGE = 216
const val PALE_RED = 217
const val ORANGE = 214
const val PALE_LIME = 156
const val LIME = 154
const val PALE_TEAL = 159
const val TEAL = 117
const val PALE_PINK = 218
const val PALE_PURPLE = 140
const val PALE_PINKISH_RED = 217
const val PINKISH_RED = 218
const val LIGHT_PINK = 219
const val WHITE = 255
fun logLn(color: Int?, text: Any?, vararg arg: Any?) = log(color, "$text\n", arg)
fun logLn(text: Any?, vararg arg: Any?) = logLn(WHITE, text, arg)

class ProtocolViolationException(message: String) : Exception(message)

fun protocolViolation(exp: Any, chk: Any, text: String) {
	if (exp != chk) throw ProtocolViolationException(format(text, "\"$chk\"", "\"$exp\""))
}

fun InputStream.read16() = (this.read() shl 8) or this.read()
fun InputStream.read24() = (this.read16() shl 8) or this.read()
fun InputStream.read32() = (this.read24() shl 8) or this.read()
fun InputStream.read64(): Long = (this.read32().toLong() shl 32) or this.read32().toLong()
fun InputStream.readInet4(): Inet4Address = Inet4Address.getByAddress(
	byteArrayOf(
		this.read().toByte(),
		this.read().toByte(),
		this.read().toByte(),
		this.read().toByte()
	)
) as Inet4Address

fun OutputStream.write16(data: Int) = this.write(data shr 8).also { this.write(data) }
fun OutputStream.write24(data: Int) = this.write(data shr 16).also { this.write16(data) }
fun OutputStream.write32(data: Int) = this.write(data shr 24).also { this.write24(data) }
fun OutputStream.writeInet4(data: Inet4Address) = data.address.forEach { this.write(it.toInt()) }