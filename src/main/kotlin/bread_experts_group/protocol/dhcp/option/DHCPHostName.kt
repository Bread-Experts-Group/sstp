package bread_experts_group.protocol.dhcp.option

import java.io.InputStream
import java.io.OutputStream

class DHCPHostName(
	val name: String
) : DHCPOption(DHCPOptionType.HOST_NAME) {
	override fun optionGist(): String = "[${name.length}] \"$name\""
	override fun calculateLength(): Int = name.length

	override fun write(stream: OutputStream) {
		super.write(stream)
		stream.write(name.encodeToByteArray())
	}

	companion object {
		fun read(stream: InputStream, length: Int): DHCPHostName = DHCPHostName(stream.readNBytes(length).decodeToString())
	}
}