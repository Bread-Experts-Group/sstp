package bread_experts_group.protocol.ppp.lcp

import bread_experts_group.util.read16
import java.io.InputStream
import java.io.OutputStream

class LCPVendorSpecific(
	identifier: Int,
	val vendorOUI: Int,
	val vendorKind: Int,
	val data: ByteArray
) : LinkControlProtocolFrame(identifier, LCPControlType.VENDOR_SPECIFIC) {
	override fun calculateLength(): Int = super.calculateLength() + 4 + data.size
	override fun write(stream: OutputStream) = TODO("Protocol rejections")

	override fun lcpGist(): String = "VEND. OUI: $vendorOUI, VEND. KIND: $vendorKind, # DATA: [${data.size}]"

	companion object {
		fun read(stream: InputStream, identifier: Int, length: Int): LCPVendorSpecific = LCPVendorSpecific(
			identifier,
			(stream.read16() shl 8) or stream.read(),
			stream.read(),
			stream.readNBytes(length - 4)
		)
	}
}