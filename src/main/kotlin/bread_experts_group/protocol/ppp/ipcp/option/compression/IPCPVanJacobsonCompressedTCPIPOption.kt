package bread_experts_group.protocol.ppp.ipcp.option.compression

import java.io.InputStream
import java.io.OutputStream

class IPCPVanJacobsonCompressedTCPIPOption(
	val maxSlotID: Int,
	val compressedSlotID: Boolean
) : IPCPCompressionOption(CompressionProtocol.VAN_JACOBSON_COMPRESSED_TCP_IP) {
	override fun calculateLength(): Int = 6

	override fun write(stream: OutputStream) {
		super.write(stream)
		stream.write(this.maxSlotID)
		stream.write(if (this.compressedSlotID) 1 else 0)
	}

	override fun optionGist(): String = "MAX SLOT ID: $maxSlotID, COMPRESSED SLOT ID: $compressedSlotID"

	companion object {
		fun read(stream: InputStream): IPCPVanJacobsonCompressedTCPIPOption = IPCPVanJacobsonCompressedTCPIPOption(
			stream.read(),
			stream.read() == 1
		)
	}
}