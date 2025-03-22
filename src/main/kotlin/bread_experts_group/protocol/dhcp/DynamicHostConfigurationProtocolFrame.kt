package bread_experts_group.protocol.dhcp

import bread_experts_group.Writable
import bread_experts_group.util.ToStringUtil.SmartToString
import java.io.InputStream

// TODO DHCP
sealed class DynamicHostConfigurationProtocolFrame : SmartToString(), Writable {
	enum class DHCPOperationType(val code: Int) {
		BOOTREQUEST(0x01),
		BOOTREPLY(0x02)
	}

	companion object {
		fun read(stream: InputStream): DynamicHostConfigurationProtocolFrame {
			return DynamicHostConfigurationProtocolFrame.read(stream)
		}
	}
}