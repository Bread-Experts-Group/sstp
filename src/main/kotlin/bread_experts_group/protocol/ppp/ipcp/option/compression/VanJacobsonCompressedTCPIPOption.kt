package bread_experts_group.protocol.ppp.ipcp.option.compression

import java.io.InputStream
import java.io.OutputStream

class VanJacobsonCompressedTCPIPOption(
	val maxSlotID: Int,
	val compressedSlotID: Boolean
) : IPCompressionProtocolOption(CompressionProtocol.VAN_JACOBSON_COMPRESSED_TCP_IP) {
	override fun calculateLength(): Int = 6

	override fun write(stream: OutputStream) {
		super.write(stream)
		stream.write(this.maxSlotID)
		stream.write(if (this.compressedSlotID) 1 else 0)
	}

	companion object {
		fun read(stream: InputStream): VanJacobsonCompressedTCPIPOption = VanJacobsonCompressedTCPIPOption(
			stream.read(),
			stream.read() == 1
		)
	}
}