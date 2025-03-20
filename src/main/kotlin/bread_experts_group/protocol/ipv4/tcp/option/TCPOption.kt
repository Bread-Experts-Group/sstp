package bread_experts_group.protocol.ipv4.tcp.option

import bread_experts_group.Writable
import bread_experts_group.util.ToStringUtil.SmartToString
import java.io.InputStream

sealed class TCPOption(val type: TCPOptionType) : SmartToString(), Writable {
	enum class TCPOptionType(val code: Int) {
		NO_OPERATION(1),
		MAXIMUM_SEGMENT_SIZE(2),
		WINDOW_SCALE(3),
		SENSITIVE_ACKNOWLEDGEMENT_ALLOWED(4),
		LAST_TIMESTAMPS(8);

		companion object {
			val mapping = entries.associateBy { it.code }
		}
	}

	companion object {
		fun read(stream: InputStream): TCPOption {
			val type = TCPOptionType.mapping.getValue(stream.read())
			if (type != TCPOptionType.NO_OPERATION) stream.skip(1)
			return when (type) {
				TCPOptionType.NO_OPERATION -> TCPNoOperationOption.read()
				TCPOptionType.MAXIMUM_SEGMENT_SIZE -> TCPMaximumSegmentSizeOption.read(stream)
				TCPOptionType.WINDOW_SCALE -> TCPWindowScaleOption.read(stream)
				TCPOptionType.SENSITIVE_ACKNOWLEDGEMENT_ALLOWED -> TCPSensitiveAcknowledgementAllowedOption.read()
				TCPOptionType.LAST_TIMESTAMPS -> TCPLastTimestampsOption.read(stream)
			}
		}
	}
}