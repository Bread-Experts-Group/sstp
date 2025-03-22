package bread_experts_group.protocol.dhcp.option

import java.io.OutputStream

class DHCPPad : DHCPOption(DHCPOptionType.PAD) {
	override fun optionGist(): String = "<>"
	override fun calculateLength(): Int = 1
	override fun write(stream: OutputStream) {
		stream.write(type.code)
	}
}