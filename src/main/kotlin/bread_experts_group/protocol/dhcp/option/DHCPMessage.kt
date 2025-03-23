package bread_experts_group.protocol.dhcp.option

import java.io.InputStream
import java.io.OutputStream

class DHCPMessage(
	val message: String
) : DHCPOption(DHCPOptionType.MESSAGE) {
	override fun optionGist(): String = "[${message.length}] \"$message\""
	override fun calculateLength(): Int = message.length

	override fun write(stream: OutputStream) {
		super.write(stream)
		stream.write(message.encodeToByteArray())
	}

	companion object {
		fun read(stream: InputStream, length: Int): DHCPMessage = DHCPMessage(stream.readNBytes(length).decodeToString())
	}
}