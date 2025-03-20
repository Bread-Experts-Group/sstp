package bread_experts_group.protocol.sstp.attribute

import bread_experts_group.protocol.sstp.attribute.CryptoBindingRequestAttribute.HashProtocol
import bread_experts_group.util.read24
import java.io.InputStream

class CryptoBindingAttribute(
	val hashProtocol: HashProtocol,
	val nonce: ByteArray,
	val hash: ByteArray,
	val compoundMAC: ByteArray
) : ControlMessageAttribute(AttributeType.SSTP_ATTRIB_CRYPTO_BINDING) {
	override fun calculateLength(): Int = 0x068

	companion object {
		fun read(stream: InputStream): CryptoBindingAttribute {
			stream.read24()
			val hashProtocol = HashProtocol.mapping.getValue(stream.read())
			val nonce = stream.readNBytes(32)
			val hash = when (hashProtocol) {
				HashProtocol.CERT_HASH_PROTOCOL_SHA1 -> stream.readNBytes(20).also { stream.skip(12) }
				HashProtocol.CERT_HASH_PROTOCOL_SHA256 -> stream.readNBytes(32)
			}
			val compoundMAC = when (hashProtocol) {
				HashProtocol.CERT_HASH_PROTOCOL_SHA1 -> stream.readNBytes(20).also { stream.skip(12) }
				HashProtocol.CERT_HASH_PROTOCOL_SHA256 -> stream.readNBytes(32)
			}
			return CryptoBindingAttribute(hashProtocol, nonce, hash, compoundMAC)
		}
	}
}