package bread_experts_group.protocol.ip.v6

import bread_experts_group.Writable
import bread_experts_group.protocol.ip.InternetProtocol
import bread_experts_group.protocol.ip.InternetProtocol.ProtocolTypes
import bread_experts_group.protocol.ip.v6.icmp.ICMPV6Frame
import bread_experts_group.util.*
import bread_experts_group.util.ToStringUtil.SmartToString
import java.io.InputStream
import java.io.OutputStream
import java.net.Inet6Address

class InternetProtocolV6Frame<T : InternetProtocol>(
	val differentiatedServices: Int,
	val congestionNotification: Int,
	val flowLabel: Int,
	val hopLimit: Int,
	val nextHeader: ProtocolTypes,
	val source: Inet6Address,
	val destination: Inet6Address,
	val data: T
) : SmartToString(), Writable {
	override fun calculateLength(): Int = 40
	override fun write(stream: OutputStream) {
		val verTC = (6 shl 28) or ((differentiatedServices and 0xFC) shl 20) or ((congestionNotification and 0x03) shl 20)
		stream.write32(verTC or (flowLabel and 0x000FFFFF))
		stream.write16(data.calculateLength())
		stream.write(nextHeader.code)
		stream.write(hopLimit)
		stream.writeInet(source)
		stream.writeInet(destination)
		data.write(stream, this)
	}

	override fun gist(): String = "$nextHeader ($source > $destination), HL: $hopLimit\n${data.gist()}"

	companion object {
		fun read(stream: InputStream): InternetProtocolV6Frame<*> {
			val (differentiatedServices, congestionNotification, flowLabel) = stream.read32().let {
				val version = it shr 28
				if (version != 6) throw IllegalStateException(version.toString())
				Triple((it shr 20) and 0xFC, (it shr 20) and 0x03, it and 0x000FFFFF)
			}
			val payloadLength = stream.read16()
			val nextHeader = ProtocolTypes.mapping.getValue(stream.read())
			val hopLimit = stream.read()
			val source = stream.readInet6()
			val destination = stream.readInet6()
			return InternetProtocolV6Frame(
				differentiatedServices,
				congestionNotification,
				flowLabel,
				hopLimit,
				nextHeader,
				source,
				destination,
				when (nextHeader) {
					ProtocolTypes.INTERNET_CONTROL_MESSAGE_PROTOCOL_V6 -> ICMPV6Frame.read(stream, payloadLength)
					else -> throw UnsupportedOperationException(nextHeader.name)
				}
			)
		}
	}
}