package bread_experts_group.protocol.dhcp.option

import bread_experts_group.Writable
import bread_experts_group.util.ToStringUtil.SmartToString
import java.io.InputStream
import java.io.OutputStream

sealed class DHCPOption(
	val type: DHCPOptionType
) : SmartToString(), Writable {
	enum class DHCPOptionType(val code: Int) {
		PAD(0),
		SUBNET_MASK(1),
		ROUTER(3),
		DOMAIN_NAME_SERVER(6),
		HOST_NAME(12),
		BROADCAST_ADDRESS(28),
		NETWORK_TIME_SERVERS(42),
		REQUEST_ADDRESS(50),
		LEASE_TIME(51),
		MESSAGE_TYPE(53),
		SERVER_ADDRESS(54),
		MESSAGE(56),
		RENEWAL_TIMER(58),
		REBINDING_TIMER(59),
		END(255);

		companion object {
			val mapping = entries.associateBy { it.code }
		}
	}

	override fun write(stream: OutputStream) {
		stream.write(type.code)
		stream.write(calculateLength())
	}

	override fun gist(): String = "OPT [${calculateLength()}] $type : ${optionGist()}"
	abstract fun optionGist(): String

	companion object {
		fun read(code: Int, stream: InputStream): DHCPOption {
			val code = DHCPOptionType.mapping.getValue(code)
			val length = stream.read()
			return when (code) {
				DHCPOptionType.PAD -> DHCPPad()
				DHCPOptionType.SUBNET_MASK -> DHCPSubnetMask.read(stream)
				DHCPOptionType.ROUTER -> DHCPRouters.read(stream, length)
				DHCPOptionType.DOMAIN_NAME_SERVER -> DHCPDomainNameServer.read(stream)
				DHCPOptionType.HOST_NAME -> DHCPHostName.read(stream, length)
				DHCPOptionType.BROADCAST_ADDRESS -> DHCPBroadcastAddress.read(stream)
				DHCPOptionType.NETWORK_TIME_SERVERS -> DHCPNetworkTimeServers.read(stream, length)
				DHCPOptionType.REQUEST_ADDRESS -> DHCPRequestAddress.read(stream)
				DHCPOptionType.LEASE_TIME -> DHCPLeaseTime.read(stream)
				DHCPOptionType.MESSAGE_TYPE -> DHCPMessageType.read(stream)
				DHCPOptionType.SERVER_ADDRESS -> DHCPServerAddress.read(stream)
				DHCPOptionType.MESSAGE -> DHCPMessage.read(stream, length)
				DHCPOptionType.RENEWAL_TIMER -> DHCPRenewalTimer.read(stream)
				DHCPOptionType.REBINDING_TIMER -> DHCPRebindingTimer.read(stream)
				DHCPOptionType.END -> DHCPEnd()
			}
		}
	}
}