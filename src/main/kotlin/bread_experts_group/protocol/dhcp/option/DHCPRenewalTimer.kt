package bread_experts_group.protocol.dhcp.option

import bread_experts_group.util.read32
import bread_experts_group.util.write32
import java.io.InputStream
import java.io.OutputStream

class DHCPRenewalTimer(
	val renewalTimerValue: Int
) : DHCPOption(DHCPOptionType.RENEWAL_TIMER) {
	override fun optionGist(): String = "${renewalTimerValue}s"
	override fun calculateLength(): Int = 4

	override fun write(stream: OutputStream) {
		super.write(stream)
		stream.write32(renewalTimerValue)
	}

	companion object {
		fun read(stream: InputStream): DHCPRenewalTimer = DHCPRenewalTimer(stream.read32())
	}
}