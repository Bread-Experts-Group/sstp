package bread_experts_group.protocol.dhcp

import bread_experts_group.Writable
import bread_experts_group.protocol.dhcp.option.DHCPEnd
import bread_experts_group.protocol.dhcp.option.DHCPOption
import bread_experts_group.protocol.dhcp.option.DHCPPad
import bread_experts_group.util.*
import bread_experts_group.util.ToStringUtil.SmartToString
import java.io.InputStream
import java.io.OutputStream
import java.net.Inet4Address

// TODO DHCP
class DynamicHostConfigurationProtocolFrame(
	val operation: DHCPOperationType,
	val hardwareType: DHCPHardwareType,
	val hardwareAddressLength: Int,
	val hops: Int,
	val identifier: Int,
	val seconds: Int,
	val flags: List<DHCPFlag>,
	val clientIP: Inet4Address,
	val yourIP: Inet4Address,
	val serverIP: Inet4Address,
	val gatewayIP: Inet4Address,
	val clientHardwareAddress: ByteArray,
	val options: List<DHCPOption>
) : SmartToString(), Writable {
	enum class DHCPOperationType(val code: Int) {
		BOOTREQUEST(0x01),
		BOOTREPLY(0x02);

		companion object {
			val mapping = entries.associateBy { it.code }
		}
	}

	enum class DHCPHardwareType(val code: Int) {
		ETHERNET_10MB(1),
		IEEE_802(6),
		ARCNET(7),
		LOCALTALK(11),
		LOCALNET(12),
		SMDS(14),
		FRAME_RELAY(15),
		ASYNCHRONOUS_TRANSFER_MODE_A(16),
		HDLC(17),
		FIBER_CHANNEL(18),
		ASYNCHRONOUS_TRANSFER_MODE_B(19),
		SERIAL_LINE(20);

		companion object {
			val mapping = entries.associateBy { it.code }
		}
	}

	enum class DHCPFlag(val position: Int) {
		BROADCAST(0b1000000000000000),
	}

	override fun calculateLength(): Int = 236 + options.sumOf { it.calculateLength() }

	override fun write(stream: OutputStream) {
		stream.write(operation.code)
		stream.write(hardwareType.code)
		stream.write(hardwareAddressLength)
		stream.write(hops)
		stream.write32(identifier)
		stream.write16(seconds)
		var flagsRaw = 0
		flags.forEach { flagsRaw = flagsRaw or it.position }
		stream.write16(flagsRaw)
		stream.write(clientIP.address)
		stream.write(yourIP.address)
		stream.write(serverIP.address)
		stream.write(gatewayIP.address)
		stream.write(clientHardwareAddress)
		stream.write(ByteArray(64 + 128))
		stream.write(99)
		stream.write(130)
		stream.write(83)
		stream.write(99)
		options.forEach { it.write(stream) }
		DHCPEnd().write(stream)
	}

	override fun gist(): String = "DHCP [${calculateLength()}] $operation [$hardwareType], ID: $identifier " +
			"(CI: $clientIP, YI: $yourIP, SI: $serverIP, GI: $gatewayIP), # OPT: [${options.size}]" + buildString {
		options.forEach {
			append("\n  ${it.gist()}")
		}
	}

	companion object {
		fun read(stream: InputStream): DynamicHostConfigurationProtocolFrame = DynamicHostConfigurationProtocolFrame(
			DHCPOperationType.mapping.getValue(stream.read()),
			DHCPHardwareType.mapping.getValue(stream.read()),
			stream.read(),
			stream.read(),
			stream.read32(),
			stream.read16(),
			stream.read16().let { f -> DHCPFlag.entries.filter { (f and it.position) > 0 } },
			stream.readInet4(),
			stream.readInet4(),
			stream.readInet4(),
			stream.readInet4(),
			stream.readNBytes(16),
			buildList {
				stream.skip(196)
				while (true) {
					val code = stream.read()
					if (code == -1) break
					val option = DHCPOption.read(code, stream)
					if (option is DHCPEnd) break
					if (option !is DHCPPad) add(option)
				}
			}
		)
	}
}