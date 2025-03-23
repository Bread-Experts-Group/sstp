package bread_experts_group.protocol.dhcp.option

import bread_experts_group.util.read32
import bread_experts_group.util.write32
import java.io.InputStream
import java.io.OutputStream

class DHCPLeaseTime(
	val leaseTimeSeconds: Int
) : DHCPOption(DHCPOptionType.LEASE_TIME) {
	override fun optionGist(): String = "${leaseTimeSeconds}s"
	override fun calculateLength(): Int = 4

	override fun write(stream: OutputStream) {
		super.write(stream)
		stream.write32(leaseTimeSeconds)
	}

	companion object {
		fun read(stream: InputStream): DHCPLeaseTime = DHCPLeaseTime(stream.read32())
	}
}