package bread_experts_group

import bread_experts_group.protocol.ipv4.IPFrame
import bread_experts_group.protocol.ipv4.IPFrame.IPFlag
import bread_experts_group.protocol.ipv4.icmp.ICMPDestinationUnreachable
import bread_experts_group.protocol.ipv4.icmp.ICMPEcho
import bread_experts_group.protocol.ipv4.icmp.ICMPFrame
import bread_experts_group.protocol.ipv4.tcp.TCPFrame
import bread_experts_group.protocol.ipv4.tcp.TCPFrame.TCPFlag
import bread_experts_group.protocol.ipv4.udp.UDPFrame
import bread_experts_group.protocol.ppp.PPPFrame
import bread_experts_group.protocol.ppp.ccp.CCPNonAcknowledgement
import bread_experts_group.protocol.ppp.ccp.CCPRequest
import bread_experts_group.protocol.ppp.ip.InternetProtocolFrameEncapsulated
import bread_experts_group.protocol.ppp.ipcp.IPCPAcknowledgement
import bread_experts_group.protocol.ppp.ipcp.IPCPNonAcknowledgement
import bread_experts_group.protocol.ppp.ipcp.IPCPRequest
import bread_experts_group.protocol.ppp.ipcp.IPCPTerminationRequest
import bread_experts_group.protocol.ppp.ipcp.option.IPAddressProtocolOption
import bread_experts_group.protocol.ppp.ipcp.option.compression.VanJacobsonCompressedTCPIPOption
import bread_experts_group.protocol.ppp.ipv6cp.IPv6CPRequest
import bread_experts_group.protocol.ppp.lcp.*
import bread_experts_group.protocol.ppp.lcp.option.AddressAndControlCompressionOption
import bread_experts_group.protocol.ppp.lcp.option.AuthenticationProtocolOption
import bread_experts_group.protocol.ppp.lcp.option.AuthenticationProtocolOption.AuthenticationProtocol
import bread_experts_group.protocol.ppp.lcp.option.MagicNumberOption
import bread_experts_group.protocol.ppp.lcp.option.ProtocolFieldCompressionOption
import bread_experts_group.protocol.ppp.pap.PAPAcknowledge
import bread_experts_group.protocol.ppp.pap.PAPRequest
import bread_experts_group.protocol.sstp.attribute.CryptoBindingRequestAttribute
import bread_experts_group.protocol.sstp.attribute.EncapsulatedProtocolAttribute.ProtocolType
import bread_experts_group.protocol.sstp.message.*
import bread_experts_group.util.*
import java.io.ByteArrayInputStream
import java.io.FileInputStream
import java.io.IOException
import java.io.OutputStream
import java.net.*
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousCloseException
import java.nio.channels.DatagramChannel
import java.security.KeyStore
import java.security.SecureRandom
import java.util.*
import javax.net.ssl.*
import kotlin.properties.Delegates
import kotlin.system.exitProcess

fun stringToInt(str: String): Int =
	if (str.substring(0, 1) == "0x") str.substring(2).toInt(16)
	else if (str.substring(0, 1) == "0b") str.substring(2).toInt(2)
	else str.toInt()

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
	NETWORK_INTERFACE_LIST("ni_list", false, false, false, ::stringToBoolean);

	companion object {
		val mapping = entries.associateBy { it.flagName }
	}
}

enum class ServerState {
	ServerConnectRequestPending,
	ServerCallConnectedPending,
	ServerCallConnected
}

enum class InterfaceState {
	POINT_TO_POINT,
	UP,
	VIRTUAL,
	LOOPBACK,
	MULTICAST
}

fun <T> Enumeration<T>.toList(): List<T> = buildList {
	while (this@toList.hasMoreElements()) add(this@toList.nextElement())
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

@OptIn(ExperimentalUnsignedTypes::class)
fun main(args: Array<String>) {
	val secureRandom = SecureRandom.getInstanceStrong()
	val singleArgs = mutableMapOf<Flags, Any>()
	val multipleArgs = mutableMapOf<Flags, MutableList<Any>>()
	logLn("Supplied Command Line Arguments")
	logLn("-------------------------------")
	val longestFlag = Flags.entries.maxOf { it.flagName.length }
	args.forEach {
		if (it[0] != '-') throw IllegalArgumentException("Bad argument \"$it\", requires - before name")
		var equIndex = it.indexOf('=')
		val flag = Flags.mapping.getValue(it.substring(1, if (equIndex == -1) it.length else equIndex))
		val value = if (equIndex == -1) "true" else it.substring(equIndex + 1)
		val asText =
			if (flag.censored) "*".repeat(value.length + secureRandom.nextInt(-value.length, value.length))
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
	logLn("===============================")
	if (singleArgs.getValue(Flags.NETWORK_INTERFACE_LIST) as Boolean) {
		val interfaces = NetworkInterface.networkInterfaces().toList()
		logLn("Available Network Interfaces (${interfaces.size})")
		logLn("-------------------------------")
		interfaces.forEach(::logInterfaceDetails)
		exitProcess(0)
	}

	logLn("Selected Network Interface")
	logLn("Use -ni_list to see all available interfaces")
	logLn("-------------------------------")
	logLn("Name resolution for DNS root server \"a.root-servers.net\" ... ")
	val remoteAddress = InetAddress.getByName("a.root-servers.net")
	logLn("Resolved: ${remoteAddress.hostAddress}")
	DatagramSocket().use {
		it.connect(remoteAddress, 7)
		logInterfaceDetails(NetworkInterface.getByInetAddress(it.localAddress))
	}
	logLn("===============================")
	logLn("Key store setup ...")
	val password = (singleArgs.getValue(Flags.KEYSTORE_PASSPHRASE) as String).toCharArray()
	val keyStore = KeyStore.getInstance("PKCS12")
	FileInputStream(singleArgs.getValue(Flags.KEYSTORE) as String).use { keyStore.load(it, password) }

	logLn("Key store initialization ...")
	val kmf = KeyManagerFactory.getInstance("SunX509")
	kmf.init(keyStore, password)

	logLn("TLS initialization ...")
	val sslContext = SSLContext.getInstance("TLS")
	sslContext.init(kmf.keyManagers, null, null)

	logLn("Server socket setup ...")
	val serverSocketFactory = sslContext.serverSocketFactory as SSLServerSocketFactory
	val serverSocket = serverSocketFactory.createServerSocket() as SSLServerSocket
	serverSocket.wantClientAuth = true

	logLn("Server socket binding ...")
	serverSocket.bind(
		InetSocketAddress(
			singleArgs.getValue(Flags.IP_ADDRESS) as String,
			singleArgs.getValue(Flags.PORT_NUMBER) as Int
		)
	)

	logLn("= Server loop =================")
	while (true) {
		val newSocket = serverSocket.accept() as SSLSocket
		Thread.ofPlatform().start {
			Thread.currentThread().name = "${newSocket.inetAddress}:${newSocket.port};${newSocket.localPort}"
			run {
				fun scanDelimiter(lookFor: String): String {
					var bucket = ""
					var pool = ""
					while (bucket.length != lookFor.length) {
						val charCode = newSocket.inputStream.read()
						if (charCode == -1) throw IOException("Communication terminated")
						val next = Char(charCode)
						if (lookFor[bucket.length] == next) bucket += next
						else {
							pool += bucket + next
							bucket = ""
						}
					}
					return pool
				}
				protocolViolation(scanDelimiter(" "), "SSTP_DUPLEX_POST", "HTTP Method was not %s, got %s")
				protocolViolation(
					scanDelimiter(" "), "/sra_{BA195980-CD49-458b-9E23-C84EE0ADCD75}/",
					"HTTP Path was not %s, got %s"
				)
				protocolViolation(scanDelimiter("\r\n"), "HTTP/1.1", "HTTP Version was not %s, got %s")
				var contentLengthOK = false
				while (true) {
					val next = scanDelimiter("\r\n")
					if (next.isEmpty()) break
					when (next.substringBefore(':')) {
						"Content-Length" -> {
							protocolViolation(
								next.substringAfter(':').trimStart(), "18446744073709551615",
								"Content-Length was not %s, got %s"
							)
							contentLengthOK = true
						}
					}
				}
				protocolViolation(contentLengthOK, true, "No Content-Length")
			}
			newSocket.outputStream.write("HTTP/1.1 200\r\nContent-Length:18446744073709551615\r\n\r\n".encodeToByteArray())
			val flushStream = object : OutputStream() {
				val buffer = mutableListOf<Byte>()
				override fun write(b: Int) {
					buffer.add(b.toByte())
				}

				override fun flush() {
					newSocket.outputStream.write(buffer.toByteArray())
					buffer.clear()
				}
			}
			var state by Delegates.observable(ServerState.ServerConnectRequestPending) { _, old, new ->
				logLn(PALE_PINK, "State switch: $old -> $new")
			}
			var magicMe = 0
			var lcpThem: LCPAcknowledgement? = null
			var lcpMe: LCPAcknowledgement? = null
			var ipcpThem: IPCPAcknowledgement? = null
			var ipcpMe: IPCPAcknowledgement? = null
			lateinit var protocol: ProtocolType

			fun handleEcho(ppp: LCPEcho) {
				logLn(PALE_LIME, "(NPND, Echo Request) > $ppp")
				PPPEncapsulate(LCPEcho(ppp.identifier, magicMe, ppp.data, false))
					.also {
						it.write(flushStream)
						logLn(LIME, "(NPND, Echo Reply) < $it")
					}
			}

			fun handleCCP(ppp: CCPRequest) {
				logLn(PALE_TEAL, "(CCP) > $ppp")
				if (ppp.options.isNotEmpty()) {
					PPPEncapsulate(CCPNonAcknowledgement(ppp.identifier, listOf(ppp.options.first())))
						.also {
							it.write(flushStream)
							logLn(PALE_PURPLE, "(CCP) < $it")
						}
				} else {
					TODO("STUFF")
				}
			}

			fun handleIPCP(ppp: IPCPRequest) {
				logLn(PALE_TEAL, "(IPCP, Link, Them) > $ppp")
				val hasVJ = ppp.options.firstOrNull { it is VanJacobsonCompressedTCPIPOption }
				if (hasVJ != null) {
					PPPEncapsulate(IPCPNonAcknowledgement(ppp.identifier, listOf(hasVJ)))
						.also {
							it.write(flushStream)
							logLn(PALE_PURPLE, "(IPCP, Link, Them) < $it")
						}
				} else {
					val ip = ppp.options.firstNotNullOfOrNull { it as? IPAddressProtocolOption }
					if (ip == null || ip.address.address.sum() == 0) {
						PPPEncapsulate(
							IPCPNonAcknowledgement(ppp.identifier, listOf(IPAddressProtocolOption(inet4(192, 168, 1, 2))))
						).also {
							it.write(flushStream)
							logLn(PALE_PURPLE, "(IPCP, Link, Them) < $it")
						}
						return
					}

					PPPEncapsulate(IPCPAcknowledgement(ppp.identifier, ppp.options)).also {
						it.write(flushStream)
						ipcpThem = it.pppFrame
						logLn(TEAL, "(IPCP, Link, Them) < $it")
						logLn(TEAL, "(IPCP, Link, Them) OK!")
					}
					PPPEncapsulate(
						IPCPRequest(
							ppp.identifier,
							listOf(IPAddressProtocolOption(Inet4Address.getLocalHost() as Inet4Address))
						)
					).also {
						it.write(flushStream)
						logLn(PALE_TEAL, "(IPCP, Link, Me) < $it")
					}
				}
			}

			fun sendICMPUnreachable(code: Int, frame: IPFrame) = IPEncapsulate(
				ICMPDestinationUnreachable(
					0, 0, 0, listOf(IPFlag.DONT_FRAGMENT),
					0, 64, frame.destination, frame.source,
					frame.asBytes(), code
				)
			).also {
				it.write(flushStream)
				logLn(PALE_PINKISH_RED, "(IP, ICMP, Destination Unreachable) < $it")
			}

			data class TCPConnection(
				val socket: Socket,
				val buffer: MutableList<ByteArray> = mutableListOf(),
				var ack: Int = -1,
				var lastAck: Int = -1,
				var seq: Int = -1,
			) {
				var removeEntry: Boolean = false
					private set

				fun close() {
					socket.close()
					buffer.clear()
					removeEntry = true
				}
			}

			val tcp = mutableMapOf<Int, TCPConnection>()
			while (true) {
				when (state) {
					ServerState.ServerConnectRequestPending -> when (val message = Message.read(newSocket.inputStream)) {
						is ConnectionRequest -> {
							logLn(GRAY, "(CPND) > $message")
							protocol = message.attribute.protocolID
							state = ServerState.ServerCallConnectedPending
							val ack = ConnectionAcknowledge(
								CryptoBindingRequestAttribute(
									listOf(CryptoBindingRequestAttribute.HashProtocol.CERT_HASH_PROTOCOL_SHA256),
									"abcdefghabcdefghabcdefghabcdefgh".encodeToByteArray()
								)
							)
							ack.write(flushStream)
							logLn(GRAY, "(CPND) < $ack")
						}

						else -> TODO(message.toString())
					}

					ServerState.ServerCallConnectedPending -> when (val message = Message.read(newSocket.inputStream)) {
						is DataMessage -> {
							when (protocol) {
								ProtocolType.SSTP_ENCAPSULATED_PROTOCOL_PPP -> {
									val ppp = PPPFrame.read(ByteArrayInputStream(message.data))
									if (lcpThem == null) {
										ppp as LCPRequest
										logLn(PALE_ORANGE, "(NPND, Link, Them) > $ppp")
										var a = ppp.options.firstOrNull { it is ProtocolFieldCompressionOption }
										var b = ppp.options.firstOrNull { it is AddressAndControlCompressionOption }
										if (a != null || b != null) {
											PPPEncapsulate(
												LCPNonAcknowledgement(
													ppp.identifier,
													buildList {
														if (a != null) add(a)
														if (b != null) add(b)
													}
												)
											).also {
												it.write(flushStream)
												logLn(PALE_RED, "(NPND, Link, Them) < $it")
											}
										} else {
											PPPEncapsulate(LCPAcknowledgement(ppp.identifier, ppp.options)).also {
												it.write(flushStream)
												lcpThem = it.pppFrame
												logLn(ORANGE, "(NPND, Link, Them) < $it")
												logLn(ORANGE, "(NPND, Link, Them) OK!")
											}
											magicMe = secureRandom.nextInt()
											PPPEncapsulate(
												LCPRequest(
													0x00,
													buildList {
														add(MagicNumberOption(magicMe))
														if (!multipleArgs[Flags.PAP_USERNAME].isNullOrEmpty())
															add(
																AuthenticationProtocolOption(
																	AuthenticationProtocol.PASSWORD_AUTHENTICATION_PROTOCOL
																)
															)
													}
												)
											).also {
												it.write(flushStream)
												logLn(PALE_BLUE, "(NPND, Link, Me) < $it")
											}
										}
									} else if (lcpMe == null) {
										if (ppp is LCPAcknowledgement) {
											logLn(BLUE, "(NPND, Link, Me) > $ppp")
											lcpMe = ppp
											logLn(BLUE, "(NPND, Link, Me) OK!")
										} else TODO(ppp.toString())
									} else when (ppp) {
										is LCPEcho -> handleEcho(ppp)
										is CCPRequest -> TODO("Reject") //handleCCP(ppp)
										is IPCPRequest -> handleIPCP(ppp)
										is IPv6CPRequest -> TODO("Reject")
										is PAPRequest -> {
											logLn(PALE_PINKISH_RED, "(PAP) > $ppp")
											val idIndex = multipleArgs.getValue(Flags.PAP_USERNAME).indexOf(ppp.peerID)
											val passphrase = multipleArgs.getValue(Flags.PAP_PASSPHRASE).getOrNull(idIndex)
											if (idIndex == -1 || (passphrase != null && ppp.password != passphrase)) {
												val err = (singleArgs.getValue(Flags.AUTHENTICATION_FAILURE_MESSAGE) as String)
													.format(ppp.peerID)
												PPPEncapsulate(PAPAcknowledge(ppp.identifier, err, false)).also {
													it.write(flushStream)
													logLn(PALE_PINKISH_RED, "(PAP) < $it")
												}
											} else {
												val ok = (singleArgs.getValue(Flags.AUTHENTICATION_SUCCESSFUL_MESSAGE) as String)
													.format(ppp.peerID)
												PPPEncapsulate(PAPAcknowledge(ppp.identifier, ok, true)).also {
													it.write(flushStream)
													logLn(LIGHT_PINK, "(PAP) < $it")
												}
											}
										}

										is LCPTerminationRequest -> TODO("LCP TERM: ${ppp.data.decodeToString()}")
										else -> TODO(ppp.toString())
									}
								}
							}
						}

						is Connected -> {
							// TODO: Confirm
							state = ServerState.ServerCallConnected
						}

						else -> TODO(message.toString())
					}

					ServerState.ServerCallConnected -> when (val message = Message.read(newSocket.inputStream)) {
						is DataMessage -> when (protocol) {
							ProtocolType.SSTP_ENCAPSULATED_PROTOCOL_PPP -> {
								val ppp = PPPFrame.read(ByteArrayInputStream(message.data))
								when (ppp) {
									is LCPEcho -> handleEcho(ppp)
									is CCPRequest -> handleCCP(ppp)
									is IPCPRequest -> handleIPCP(ppp)
									is IPCPAcknowledgement -> {
										logLn(TEAL, "(IPCP, Link, Me) > $ppp")
										logLn(TEAL, "(IPCP, Link, Me) OK!")
										ipcpMe = ppp
									}

									is InternetProtocolFrameEncapsulated -> when (ppp.frame) {
										is ICMPEcho -> {
											if (ppp.frame.type == ICMPFrame.ICMPType.ECHO_REQUEST) {
												logLn(PALE_PINK, "(IP, ICMP, Echo Request) > ${ppp.frame}")
												val actualReq = ppp.frame.destination.isReachable(1000)
												if (actualReq) IPEncapsulate(
													ICMPEcho(
														0, 0, 0, listOf(IPFlag.DONT_FRAGMENT),
														0, 64, ppp.frame.destination, ppp.frame.source,
														ppp.frame.echoIdentifier, ppp.frame.echoSequence,
														ppp.frame.data, false
													)
												).also {
													it.write(flushStream)
													logLn(PALE_PINKISH_RED, "(IP, ICMP, Echo Reply) < $it")
												} else sendICMPUnreachable(1, ppp.frame)
											}
										}

										is TCPFrame -> {
											fun logTCP(color: Int, frame: TCPFrame) {
												logLn(
													color,
													"(IP, TCP) ${frame.source}:${frame.sourcePort} > " +
															"${frame.destination}:${frame.destPort} | " +
															"[${frame.tcpFlags.joinToString(",")}] [${frame.data.size}]"
												)
											}

											fun sendTCP(frame: TCPFrame) {
												IPEncapsulate(frame).write(flushStream)
												logTCP(PALE_PINK, frame)
											}

											fun sendFrame(
												connection: TCPConnection,
												vararg flag: TCPFlag,
												data: ByteArray = byteArrayOf()
											) = sendTCP(
												TCPFrame(
													0, 0, 0, listOf(IPFlag.DONT_FRAGMENT),
													0, 64, ppp.frame.destination, ppp.frame.source,
													ppp.frame.destPort, ppp.frame.sourcePort,
													connection.seq, connection.ack,
													listOf(*flag),
													64240, 0, 0, listOf(),
													data
												)
											)

											fun sendReset() {
												sendTCP(
													TCPFrame(
														0, 0, 0, listOf(IPFlag.DONT_FRAGMENT),
														0, 64, ppp.frame.destination, ppp.frame.source,
														ppp.frame.destPort, ppp.frame.sourcePort,
														ppp.frame.acknowledgementNumber, ppp.frame.sequence,
														listOf(TCPFlag.RST),
														64240, 0, 0, listOf(),
														byteArrayOf()
													)
												)
												tcp.remove(ppp.frame.sourcePort)
											}

											logTCP(PALE_PINKISH_RED, ppp.frame)
											val connection = tcp[ppp.frame.sourcePort]
											if (connection == null) {
												if (ppp.frame.flags.size == 1 && ppp.frame.tcpFlags[0] == TCPFlag.SYN) {
													try {
														val socket = Socket(ppp.frame.destination, ppp.frame.destPort)
														val newConnection = TCPConnection(socket)
														Thread.ofPlatform().start {
															val array = ByteArray(200)
															try {
																while (true) {
																	val readCnt = socket.inputStream.read(array)
																	if (readCnt == -1) break
																	sendFrame(
																		newConnection, TCPFlag.ACK, TCPFlag.PSH,
																		data = array.sliceArray(0 until readCnt)
																	)
																	newConnection.seq += readCnt
																}
															} catch (_: SocketException) {
															}
														}
														tcp[ppp.frame.sourcePort] = newConnection
														newConnection.ack = ppp.frame.sequence + 1
														newConnection.lastAck = newConnection.ack
														newConnection.seq = secureRandom.nextInt()
														sendFrame(newConnection, TCPFlag.SYN, TCPFlag.ACK)
														newConnection.seq++
													} catch (_: SocketException) {
														sendICMPUnreachable(1, ppp.frame)
													}
												} else sendReset()
											} else {
												var send = true
												val flags = mutableListOf<TCPFlag>()
												if (ppp.frame.tcpFlags.contains(TCPFlag.ACK)) {
													if (ppp.frame.data.isNotEmpty()) {
														connection.ack += ppp.frame.data.size
														connection.buffer.add(ppp.frame.data)
														flags.add(TCPFlag.ACK)
													}
													if (connection.removeEntry) tcp.remove(ppp.frame.sourcePort)
												} else {
													sendReset()
													send = false
												}
												if (ppp.frame.tcpFlags.contains(TCPFlag.PSH)) {
													try {
														connection.buffer.removeIf {
															connection.socket.outputStream.write(it)
															true
														}
													} catch (_: SocketException) {
														sendReset()
														send = false
													}
												}
												if (ppp.frame.tcpFlags.contains(TCPFlag.FIN)) {
													connection.ack += 1
													flags.add(TCPFlag.ACK)
													flags.add(TCPFlag.FIN)
													connection.close()
												}
												if (send && flags.isNotEmpty()) sendFrame(connection, *flags.toTypedArray())
											}
										}

										is UDPFrame -> {
											logLn(PALE_LIME, "(IP, UDP) > ${ppp.frame}")
											Thread.ofPlatform().start {
												val channel = DatagramChannel.open()
												channel.connect(
													InetSocketAddress(
														ppp.frame.destination,
														ppp.frame.destPort
													)
												)
												channel.write(ByteBuffer.wrap(ppp.frame.data))
												var seq = 0
												try {
													while (true) {
														Thread.ofVirtual().start {
															val savSeq = seq
															Thread.sleep(30000) // TODO UDP Timeout
															@Suppress("KotlinConstantConditions")
															if (savSeq == seq) channel.close()
														}
														val packet = ByteBuffer.allocate(65535)
														val read = ByteArray(channel.read(packet))
														packet.flip()
														packet.get(read)
														IPEncapsulate(
															UDPFrame(
																0, 0, 0, listOf(IPFlag.DONT_FRAGMENT),
																0, 64,
																ppp.frame.destination, ppp.frame.source,
																ppp.frame.destPort, ppp.frame.sourcePort,
																0, read
															)
														).also {
															it.write(flushStream)
															logLn(LIME, "(IP, UDP) < $it")
														}
														seq++
													}
												} catch (_: AsynchronousCloseException) {
												} catch (_: PortUnreachableException) {
													channel.close()
													sendICMPUnreachable(3, ppp.frame)
												}
											}
										}

										else -> TODO(ppp.frame.toString())
									}

									is LCPTerminationRequest -> TODO("LCP TERM: ${ppp.data.decodeToString()}")
									is IPCPTerminationRequest -> TODO("IPCP TERM: ${ppp.data.decodeToString()}")
									else -> TODO(ppp.toString())
								}
							}
						}

						else -> TODO(message.toString())
					}
				}
			}
			newSocket.close()
		}.uncaughtExceptionHandler = Thread.UncaughtExceptionHandler { thread, exp ->
			logLn(PALE_RED, exp.stackTraceToString())
			newSocket.close()
		}
	}
}