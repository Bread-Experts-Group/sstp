package bread_experts_group.protocol.ppp.lcp

import bread_experts_group.protocol.ppp.PointToPointProtocolFrame
import bread_experts_group.util.read16
import bread_experts_group.util.write16
import java.io.InputStream
import java.io.OutputStream

sealed class LinkControlProtocolFrame(
	val identifier: Int,
	val type: LCPControlType
) : PointToPointProtocolFrame(PPPProtocol.LINK_CONTROL_PROTOCOL) {
	override fun calculateLength(): Int = 4

	override fun write(stream: OutputStream) {
		super.write(stream)
		stream.write(this.type.code)
		stream.write(this.identifier)
		stream.write16(this.calculateLength())
	}

	final override fun protocolGist(): String = "$type, ID: $identifier\n${lcpGist()}"
	abstract fun lcpGist(): String

	@Suppress("serial")
	class UnrecognizedControlType(val code: Int) : Exception()

	companion object {
		fun read(stream: InputStream): LinkControlProtocolFrame {
			val codeRaw = stream.read()
			val code = try {
				LCPControlType.mapping.getValue(codeRaw)
			} catch (_: NoSuchElementException) {
				throw UnrecognizedControlType(codeRaw)
			}
			val id = stream.read()
			val length = stream.read16() - 4
			return when (code) {
				LCPControlType.VENDOR_SPECIFIC -> LCPVendorSpecific.read(stream, length, id)
				LCPControlType.CONFIGURE_REQUEST -> LCPRequest(stream, length, id)
				LCPControlType.CONFIGURE_NAK -> LCPNonAcknowledgement(stream, length, id)
				LCPControlType.CONFIGURE_REJECT -> LCPRejection(stream, length, id)
				LCPControlType.CONFIGURE_ACK -> LCPAcknowledgement(stream, length, id)
				LCPControlType.ECHO_REQUEST, LCPControlType.ECHO_REPLY -> LCPEcho.read(stream, id, length)
				LCPControlType.TERMINATE_REQUEST -> LCPTermination.read(stream, id, length, true)
				LCPControlType.TERMINATE_ACK -> LCPTermination.read(stream, id, length, false)
				LCPControlType.PROTOCOL_REJECT -> LCPProtocolRejection.read(stream, id, length)
				LCPControlType.CODE_REJECT -> LCPCodeRejection.read(stream, id, length)
				LCPControlType.DISCARD_REQUEST -> LCPDiscardRequest.read(stream, id, length)
			}
		}
	}
}