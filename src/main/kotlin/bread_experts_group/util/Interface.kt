package bread_experts_group.util

import java.net.NetworkInterface

enum class InterfaceState {
	POINT_TO_POINT,
	UP,
	VIRTUAL,
	LOOPBACK,
	MULTICAST
}

fun logInterfaceDetails(face: NetworkInterface) {
	val state = buildList {
		if (face.isPointToPoint) add(InterfaceState.POINT_TO_POINT)
		if (face.isUp) add(InterfaceState.UP)
		if (face.isVirtual) add(InterfaceState.VIRTUAL)
		if (face.isLoopback) add(InterfaceState.LOOPBACK)
		if (face.supportsMulticast()) add(InterfaceState.MULTICAST)
	}.joinToString(",")
	logLn("(${face.index}) ${face.name} [$state]")
	logLn("  Display Name    : ${face.displayName}")
	face.hardwareAddress?.let {
		logLn("  Hrdw Address    : ${face.hardwareAddress.joinToString(":") { it.toUByte().toString(16) }}")
	}
	logLn("  MTU             : ${face.mtu}")
	logLn("  Intf. Addresses : (${face.interfaceAddresses.size})")
	face.interfaceAddresses.forEach {
		logLn("    ${it.toString().replace("%", "%%")}")
	}
	val subInterfaces = face.subInterfaces.toList()
	logLn("  Sub-interfaces  : (${subInterfaces.size})")
	subInterfaces.forEach { logInterfaceDetails(it) }
	face.parent?.let { logLn("  Parent       : (${it.index}) ${it.name}") }
}