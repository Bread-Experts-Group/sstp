package bread_experts_group.protocol.sstp.attribute

import bread_experts_group.util.read24
import bread_experts_group.util.write24
import java.io.InputStream
import java.io.OutputStream

class CryptoBindingRequestAttribute(
	val hashProtocols: List<HashProtocol>,
	val nonce: ByteArray
) : ControlMessageAttribute(AttributeType.SSTP_ATTRIB_CRYPTO_BINDING_REQ) {
	enum class HashProtocol(val position: Int) {
		CERT_HASH_PROTOCOL_SHA1(0b00000001),
		CERT_HASH_PROTOCOL_SHA256(0b00000010);

		companion object {
			val mapping = entries.associateBy { it.position }
		}
	}

	override fun calculateLength(): Int = 0x028

	override fun write(stream: OutputStream) {
		super.write(stream)
		stream.write24(0)
		var codedHashes = 0
		this.hashProtocols.forEach { codedHashes = codedHashes or it.position }
		stream.write(codedHashes)
		stream.write(nonce, 0, 32)
		stream.flush()
	}

	companion object {
		fun read(stream: InputStream): CryptoBindingRequestAttribute {
			stream.read24()
			val hashes = stream.read().let { mask -> HashProtocol.entries.filter { (mask and it.position) > 0 } }
			return CryptoBindingRequestAttribute(hashes, stream.readNBytes(32))
		}
	}
}