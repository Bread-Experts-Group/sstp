package bread_experts_group.protocol.sstp.attribute

import bread_experts_group.protocol.sstp.attribute.SSTPCryptoBindingRequestAttribute.HashProtocol
import bread_experts_group.util.read24
import java.io.InputStream

@Suppress("unused") // TODO verification
class SSTPCryptoBindingAttribute(
	val hashProtocol: HashProtocol,
	val nonce: ByteArray,
	val hash: ByteArray,
	val compoundMAC: ByteArray
) : SSTPControlMessageAttribute(AttributeType.SSTP_ATTRIB_CRYPTO_BINDING) {
	override fun calculateLength(): Int = 0x068

	companion object {
		fun read(stream: InputStream): SSTPCryptoBindingAttribute {
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
			return SSTPCryptoBindingAttribute(hashProtocol, nonce, hash, compoundMAC)
		}
	}
}