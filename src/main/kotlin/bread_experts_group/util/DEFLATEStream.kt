package bread_experts_group.util

import java.io.InputStream
import java.util.*

@Suppress("unused") // TODO work on later, CCP stabilization
class DEFLATEStream(private val stream: InputStream) : InputStream() {
	private val internalBuffer = Stack<Int>()
	private var handlingByte: Int = 0
	private var positionInByte: Int = 8
	private fun nextBit(): Boolean {
		if (positionInByte > 7) {
			handlingByte = stream.read()
			positionInByte = 0
		}
		val read = (handlingByte and (1 shl positionInByte)) > 0
		positionInByte++
		return read
	}

	override fun read(): Int {
		if (internalBuffer.isNotEmpty()) return internalBuffer.pop()
		nextBit() // last block
		if (nextBit()) {
			if (nextBit()) throw IllegalStateException("Reserved block")
			else staticHuffman()
		} else {
			if (nextBit()) {
				TODO("Dynamic huffman")
			} else {
				TODO("No compression")
			}
		}
		return internalBuffer.pop()
	}

	fun staticHuffman() {
		while (true) {
			var value = 0
			var bits = 0
			fun push(b: Int) {
				value = 0
				bits = 0
				internalBuffer.push(b)
			}

			while (true) {
				value = (value shl 1) or (if (nextBit()) 1 else 0)
				bits++
				when {
					bits == 8 && value in 48..191 -> push(value - 48)
					bits == 9 && value in 400..511 -> push(value - 400)
					bits == 7 && value in 0..23 -> when (value) {
						0 -> return
						else -> TODO(value.toString())
					}

					bits == 8 && value in 192..199 -> TODO(value.toString())
					bits > 9 -> throw IllegalStateException("read too many bits")
					else -> null
				}
			}
		}
	}
}