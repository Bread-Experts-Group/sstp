package bread_experts_group.util

import kotlin.random.Random

var verbosity = -1

fun stringToLong(str: String): Long =
	if (str.substring(0, 1) == "0x") str.substring(2).toLong(16)
	else if (str.substring(0, 1) == "0b") str.substring(2).toLong(2)
	else str.toLong()

fun stringToInt(str: String): Int = stringToLong(str).toInt()


fun stringToBoolean(str: String): Boolean = str.lowercase().let { it == "true" || it == "yes" || it == "1" }

enum class Flags(
	val flagName: String, val censored: Boolean, val repeatable: Boolean, val default: Any?,
	val conv: ((String) -> Any) = { it }
) {
	IP_ADDRESS("ip", false, false, "0.0.0.0"),
	PORT_NUMBER("port", false, false, 443, ::stringToInt),
	KEYSTORE("keystore", false, false, null),
	KEYSTORE_PASSPHRASE("keystore_passphrase", true, false, null),
	AUTHENTICATION_SUCCESSFUL_MESSAGE("auth_ok_msg", false, false, "Authentication OK, %s"),
	AUTHENTICATION_FAILURE_MESSAGE("auth_bad_msg", false, false, "Authentication FAIL, %s"),
	PAP_USERNAME("pap_username", false, true, null),
	PAP_PASSPHRASE("pap_passphrase", true, true, null),
	AUTHENTICATION_TRIES("auth_tries", false, false, 3, ::stringToInt),
	UDP_TIMEOUT("udp_timeout", false, false, ::stringToInt),
	TCP_TIMEOUT("tcp_timeout", false, false, ::stringToInt),
	ICMP_TIMEOUT("icmp_timeout", false, false, ::stringToInt),
	NETWORK_INTERFACE_LIST("ni_list", false, false, false, ::stringToBoolean),
	VERBOSITY("verbosity", false, false, 0, ::stringToInt),
	SECURE_RANDOM_GENERATOR("random", false, false, null);

	companion object {
		val mapping = entries.associateBy { it.flagName }
	}
}

typealias SingleArgs = Map<Flags, Any>
typealias MultipleArgs = Map<Flags, List<Any>>
typealias Args = Pair<SingleArgs, MultipleArgs>
fun readArgs(args: Array<String>): Args {
	val singleArgs = mutableMapOf<Flags, Any>()
	val multipleArgs = mutableMapOf<Flags, MutableList<Any>>()
	val longestFlag = Flags.entries.maxOf { it.flagName.length }
	args.forEach {
		if (it[0] != '-') throw IllegalArgumentException("Bad argument \"$it\", requires - before name")
		var equIndex = it.indexOf('=')
		val flag = Flags.mapping.getValue(it.substring(1, if (equIndex == -1) it.length else equIndex))
		val value = if (equIndex == -1) "true" else it.substring(equIndex + 1)
		val asText =
			if (flag.censored) "*".repeat(value.length + Random.nextInt(-value.length, value.length))
			else value
		logLn("${flag.flagName.padEnd(longestFlag)} : $asText")
		val typedValue = if (value.isNotBlank()) flag.conv(value) else flag.default
		if (typedValue != null) {
			if (flag.repeatable) {
				multipleArgs
					.getOrPut(flag) { mutableListOf() }
					.add(typedValue)
			} else {
				if (singleArgs.putIfAbsent(flag, typedValue) != null)
					throw IllegalArgumentException("Duplicate flag, \"${flag.flagName}\"")
			}
		}
	}
	Flags.entries.forEach {
		if (!it.repeatable && it.default != null && !singleArgs.contains(it)) {
			singleArgs.put(it, it.default)
			logLn("${it.flagName.padEnd(longestFlag)} : ${it.default}")
		}
	}
	return singleArgs to multipleArgs
}