package bread_experts_group.protocol.dhcp.option

import bread_experts_group.util.read32
import bread_experts_group.util.write32
import java.io.InputStream
import java.io.OutputStream

class DHCPRebindingTimer(
	val rebindindTimerValue: Int
) : DHCPOption(DHCPOptionType.REBINDING_TIMER) {
	override fun optionGist(): String = "${rebindindTimerValue}s"
	override fun calculateLength(): Int = 4

	override fun write(stream: OutputStream) {
		super.write(stream)
		stream.write32(rebindindTimerValue)
	}

	companion object {
		fun read(stream: InputStream): DHCPRebindingTimer = DHCPRebindingTimer(stream.read32())
	}
}