package bread_experts_group

import bread_experts_group.protocol.ipv4.IPFrame.IPFlag
import bread_experts_group.protocol.ipv4.icmp.ICMPDestinationUnreachable
import bread_experts_group.protocol.ipv4.icmp.ICMPEcho
import bread_experts_group.protocol.ipv4.icmp.ICMPFrame
import bread_experts_group.protocol.ipv4.tcp.TCPFrame
import bread_experts_group.protocol.ipv4.tcp.TCPFrame.TCPFlag
import bread_experts_group.protocol.ipv4.udp.UDPFrame
import bread_experts_group.protocol.ppp.PPPFrame
import bread_experts_group.protocol.ppp.ccp.CompressionControlConfigurationNonAcknowledgement
import bread_experts_group.protocol.ppp.ccp.CompressionControlConfigurationRequest
import bread_experts_group.protocol.ppp.ip.IPFrameEncapsulated
import bread_experts_group.protocol.ppp.ipcp.InternetProtocolControlConfigurationAcknowledgement
import bread_experts_group.protocol.ppp.ipcp.InternetProtocolControlConfigurationNonAcknowledgement
import bread_experts_group.protocol.ppp.ipcp.InternetProtocolControlConfigurationRequest
import bread_experts_group.protocol.ppp.ipcp.InternetProtocolControlTerminationRequest
import bread_experts_group.protocol.ppp.ipcp.option.IPAddressProtocolOption
import bread_experts_group.protocol.ppp.ipcp.option.compression.VanJacobsonCompressedTCPIPOption
import bread_experts_group.protocol.ppp.ipv6cp.InternetProtocolV6ControlConfigurationRequest
import bread_experts_group.protocol.ppp.lcp.*
import bread_experts_group.protocol.ppp.lcp.option.AddressAndControlCompressionOption
import bread_experts_group.protocol.ppp.lcp.option.AuthenticationProtocolOption
import bread_experts_group.protocol.ppp.lcp.option.AuthenticationProtocolOption.AuthenticationProtocol
import bread_experts_group.protocol.ppp.lcp.option.MagicNumberOption
import bread_experts_group.protocol.ppp.lcp.option.ProtocolFieldCompressionOption
import bread_experts_group.protocol.ppp.pap.PasswordAuthenticationAcknowledge
import bread_experts_group.protocol.ppp.pap.PasswordAuthenticationRequest
import bread_experts_group.protocol.sstp.attribute.CryptoBindingRequestAttribute
import bread_experts_group.protocol.sstp.attribute.EncapsulatedProtocolAttribute.ProtocolType
import bread_experts_group.protocol.sstp.message.*
import bread_experts_group.util.*
import java.io.*
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
			var lcpThem: LinkControlConfigurationAcknowledgement? = null
			var lcpMe: LinkControlConfigurationAcknowledgement? = null
			var ipcpThem: InternetProtocolControlConfigurationAcknowledgement? = null
			var ipcpMe: InternetProtocolControlConfigurationAcknowledgement? = null
			lateinit var protocol: ProtocolType

			fun handleEcho(ppp: LinkControlEcho) = ByteArrayOutputStream().use {
				logLn(PALE_LIME, "(NPND, Echo Request) > $ppp")
				val rep = LinkControlEcho(
					0xFF, 0x03, ppp.identifier,
					magicMe, ppp.data, false
				)
				rep.write(it)
				DataMessage(it.toByteArray()).write(flushStream)
				logLn(LIME, "(NPND, Echo Reply) < $rep")
			}

			fun handleCCP(ppp: CompressionControlConfigurationRequest) = ByteArrayOutputStream().use {
				logLn(PALE_TEAL, "(CCP) > $ppp")
				if (ppp.options.isNotEmpty()) {
					val nak = CompressionControlConfigurationNonAcknowledgement(
						0xFF, 0x03, ppp.identifier,
						listOf(ppp.options.first())
					)
					nak.write(it)
					DataMessage(it.toByteArray()).write(flushStream)
					logLn(PALE_PURPLE, "(CCP) < $nak")
				} else {
					TODO("STUFF")
				}
			}

			fun handleIPCP(ppp: InternetProtocolControlConfigurationRequest) = ByteArrayOutputStream().use { topStream ->
				logLn(PALE_TEAL, "(IPCP, Link, Them) > $ppp")
				val hasVJ = ppp.options.firstOrNull { it is VanJacobsonCompressedTCPIPOption }
				if (hasVJ != null) {
					val nak = InternetProtocolControlConfigurationNonAcknowledgement(
						0xFF, 0x03, ppp.identifier,
						listOf(hasVJ)
					)
					nak.write(topStream)
					DataMessage(topStream.toByteArray()).write(flushStream)
					logLn(PALE_PURPLE, "(IPCP, Link, Them) < $nak")
				} else {
					val ip = ppp.options.firstNotNullOfOrNull { it as? IPAddressProtocolOption }
					if (ip == null || ip.address.address.sum() == 0) {
						val nak = InternetProtocolControlConfigurationNonAcknowledgement(
							0xFF, 0x03, ppp.identifier,
							listOf(
								IPAddressProtocolOption(
									Inet4Address.getByAddress(ubyteArrayOf(192u, 168u, 1u, 2u).toByteArray())
											as Inet4Address
								)
							)
						)
						nak.write(topStream)
						DataMessage(topStream.toByteArray()).write(flushStream)
						logLn(PALE_PURPLE, "(IPCP, Link, Them) < $nak")
						return
					}

					ByteArrayOutputStream().use {
						val ack = InternetProtocolControlConfigurationAcknowledgement(
							0xFF, 0x03, ppp.identifier,
							ppp.options
						)
						ack.write(it)
						DataMessage(it.toByteArray()).write(flushStream)
						ipcpThem = ack
						logLn(TEAL, "(IPCP, Link, Them) < $ack")
						logLn(TEAL, "(IPCP, Link, Them) OK!")
					}
					ByteArrayOutputStream().use {
						val req = InternetProtocolControlConfigurationRequest(
							0xFF, 0x03, ppp.identifier,
							listOf(
								IPAddressProtocolOption(
									Inet4Address.getLocalHost()
											as Inet4Address
								)
							)
						)
						req.write(it)
						DataMessage(it.toByteArray()).write(flushStream)
						logLn(PALE_TEAL, "(IPCP, Link, Me) < $req")
					}
				}
			}

			data class TCPConnection(
				val socket: Socket,
				val buffer: MutableList<ByteArray>,
				var ack: Int,
				var lastAck: Int,
				var seq: Int,
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
										ppp as LinkControlConfigurationRequest
										logLn(PALE_ORANGE, "(NPND, Link, Them) > $ppp")
										var a = ppp.options.firstOrNull { it is ProtocolFieldCompressionOption }
										var b = ppp.options.firstOrNull { it is AddressAndControlCompressionOption }
										if (a != null || b != null) {
											ByteArrayOutputStream().use {
												val nak = LinkControlConfigurationNonAcknowledgement(
													0xFF, 0x03, ppp.identifier,
													buildList {
														if (a != null) add(a)
														if (b != null) add(b)
													}
												)
												nak.write(it)
												DataMessage(it.toByteArray()).write(flushStream)
												logLn(PALE_RED, "(NPND, Link, Them) < $nak")
											}
										} else {
											ByteArrayOutputStream().use {
												val ack = LinkControlConfigurationAcknowledgement(
													0xFF, 0x03, ppp.identifier,
													ppp.options
												)
												ack.write(it)
												DataMessage(it.toByteArray()).write(flushStream)
												logLn(ORANGE, "(NPND, Link, Them) < $ack")
												lcpThem = ack
												logLn(ORANGE, "(NPND, Link, Them) OK!")
											}
											ByteArrayOutputStream().use {
												magicMe = secureRandom.nextInt()
												val req = LinkControlConfigurationRequest(
													0xFF, 0x03, 0x00,
													buildList {
														add(MagicNumberOption(magicMe))
														if (!multipleArgs[Flags.PAP_USERNAME].isNullOrEmpty()) {
															add(
																AuthenticationProtocolOption(
																	AuthenticationProtocol.PASSWORD_AUTHENTICATION_PROTOCOL
																)
															)
														}
													}
												)
												req.write(it)
												DataMessage(it.toByteArray()).write(flushStream)
												logLn(PALE_BLUE, "(NPND, Link, Me) < $req")
											}
										}
									} else if (lcpMe == null) {
										if (ppp is LinkControlConfigurationAcknowledgement) {
											logLn(BLUE, "(NPND, Link, Me) > $ppp")
											lcpMe = ppp
											logLn(BLUE, "(NPND, Link, Me) OK!")
										} else TODO(ppp.toString())
									} else when (ppp) {
										is LinkControlEcho -> handleEcho(ppp)
										is CompressionControlConfigurationRequest -> TODO("Reject") //handleCCP(ppp)
										is InternetProtocolControlConfigurationRequest -> handleIPCP(ppp)
										is InternetProtocolV6ControlConfigurationRequest -> TODO("Reject")
										is PasswordAuthenticationRequest -> {
											ByteArrayOutputStream().use {
												logLn(PALE_PINKISH_RED, "(PAP) > $ppp")
												val idIndex = multipleArgs.getValue(Flags.PAP_USERNAME).indexOf(ppp.peerID)
												val passphrase = multipleArgs.getValue(Flags.PAP_PASSPHRASE).getOrNull(idIndex)
												if (idIndex == -1 || (passphrase != null && ppp.password != passphrase)) {
													val nak = PasswordAuthenticationAcknowledge(
														0xFF, 0x03, 0x00,
														(singleArgs.getValue(Flags.AUTHENTICATION_FAILURE_MESSAGE) as String)
															.format(ppp.peerID),
														false
													)
													nak.write(it)
													logLn(PINKISH_RED, "(PAP) < $nak")
												} else {
													val ack = PasswordAuthenticationAcknowledge(
														0xFF, 0x03, 0x00,
														(singleArgs.getValue(Flags.AUTHENTICATION_SUCCESSFUL_MESSAGE) as String)
															.format(ppp.peerID),
														true
													)
													ack.write(it)
													logLn(LIGHT_PINK, "(PAP) < $ack")
												}
												DataMessage(it.toByteArray()).write(flushStream)
												flushStream.flush()
											}
										}

										is LinkControlTerminationRequest ->
											TODO("LCP TERM: ${ppp.data.decodeToString()}")

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
						is DataMessage -> {
							when (protocol) {
								ProtocolType.SSTP_ENCAPSULATED_PROTOCOL_PPP -> {
									val ppp = PPPFrame.read(ByteArrayInputStream(message.data))
									when (ppp) {
										is LinkControlEcho -> handleEcho(ppp)
										is CompressionControlConfigurationRequest -> handleCCP(ppp)
										is InternetProtocolControlConfigurationRequest -> handleIPCP(ppp)
										is InternetProtocolControlConfigurationAcknowledgement -> {
											logLn(TEAL, "(IPCP, Link, Me) > $ppp")
											logLn(TEAL, "(IPCP, Link, Me) OK!")
											ipcpMe = ppp
										}

										is IPFrameEncapsulated -> when (ppp.frame) {
											is ICMPEcho -> {
												if (ppp.frame.type == ICMPFrame.ICMPType.ECHO_REQUEST) {
													logLn(PALE_PINK, "(IP, ICMP, Echo Request) > ${ppp.frame}")
													val actualReq = ppp.frame.destination.isReachable(1000)
													ByteArrayOutputStream().use {
														if (actualReq) {
															val reply = ICMPEcho(
																0, 0, 0, listOf(IPFlag.DONT_FRAGMENT),
																0, 64, ppp.frame.destination, ppp.frame.source,
																ppp.frame.echoIdentifier, ppp.frame.echoSequence,
																ppp.frame.data, false
															)
															val encap = IPFrameEncapsulated(reply)
															encap.write(it)
															logLn(
																PINKISH_RED,
																"(IP, ICMP, Echo Reply) < $reply"
															)
														} else {
															val original = ByteArrayOutputStream()
																.also { ppp.frame.write(it) }
																.toByteArray()
															val unreachable = ICMPDestinationUnreachable(
																0, 0, 0, listOf(IPFlag.DONT_FRAGMENT),
																0, 64, ppp.frame.destination, ppp.frame.source,
																original, 1
															)
															val encap = IPFrameEncapsulated(unreachable)
															encap.write(it)
															logLn(
																PINKISH_RED,
																"(IP, ICMP, Destination Unreachable) < $unreachable"
															)
														}
														DataMessage(it.toByteArray()).write(flushStream)
														flushStream.flush()
													}
												} else TODO("REPLY")
											}

											is TCPFrame -> {
												fun logTCP(frame: TCPFrame) {
													logLn(
														PINKISH_RED,
														"(IP, TCP) ${frame.source}:${frame.sourcePort} > " +
																"${frame.destination}:${frame.destPort} | " +
																"[${frame.tcpFlags.joinToString(",")}] [${frame.data.size}]"
													)
												}

												logTCP(ppp.frame)
												val connection = tcp[ppp.frame.sourcePort]

												fun createRespondFrame(
													connection: TCPConnection,
													data: ByteArray,
													vararg flag: TCPFlag
												) = TCPFrame(
													0, 0, 0, listOf(IPFlag.DONT_FRAGMENT),
													0, 64, ppp.frame.destination, ppp.frame.source,
													ppp.frame.destPort, ppp.frame.sourcePort,
													connection.seq, connection.ack,
													listOf(*flag),
													64240, 0, 0, listOf(),
													data
												)

												if (
													ppp.frame.tcpFlags.contains(TCPFlag.SYNCHRONIZE_SEQUENCE) &&
													ppp.frame.tcpFlags.size == 1
												) {
													val connection = TCPConnection(Socket(), mutableListOf(), -1, -1, -1)
													tcp[ppp.frame.sourcePort] = connection
													connection.seq = secureRandom.nextInt()
													connection.socket.connect(
														InetSocketAddress(
															ppp.frame.destination,
															ppp.frame.destPort
														),
														1000
													)
													Thread.ofPlatform().name("${Thread.currentThread().name}-Sock").start {
														val array = ByteArray(64240)
														try {
															while (connection.socket.isConnected) {
																ByteArrayOutputStream().use {
																	val read = connection.socket.inputStream.read(array)
																	val data = createRespondFrame(
																		connection,
																		array.sliceArray(0..(read - 1)),
																		TCPFlag.PUSH_BUFFER, TCPFlag.ACKNOWLEDGEMENT_NUMBER
																	)
																	connection.seq += read
																	val encap = IPFrameEncapsulated(data)
																	encap.write(it)
																	DataMessage(it.toByteArray()).write(flushStream)
																	flushStream.flush()
																	logTCP(data)
																}
															}
														} catch (_: SocketException) {
															logLn(PINKISH_RED, "(IP, TCP) Goodbye")
														}
													}
													ByteArrayOutputStream().use {
														connection.ack = ppp.frame.sequence + 1
														connection.lastAck = connection.ack
														val synAck = createRespondFrame(
															connection,
															byteArrayOf(),
															TCPFlag.SYNCHRONIZE_SEQUENCE, TCPFlag.ACKNOWLEDGEMENT_NUMBER
														)
														val encap = IPFrameEncapsulated(synAck)
														encap.write(it)
														DataMessage(it.toByteArray()).write(flushStream)
														flushStream.flush()
														logTCP(synAck)
													}
													connection.seq += 1
												} else if (ppp.frame.tcpFlags.contains(TCPFlag.ACKNOWLEDGEMENT_NUMBER)) {
													if (connection != null && !connection.socket.isClosed) {
														connection.buffer.add(ppp.frame.data)
														connection.ack = ppp.frame.sequence
														if (ppp.frame.tcpFlags.contains(TCPFlag.PUSH_BUFFER)) {
															connection.buffer.removeAll {
																connection.socket.outputStream.write(it)
																true
															}
															connection.socket.outputStream.flush()
														}
														if (connection.ack != connection.lastAck) {
															connection.lastAck = connection.ack
															ByteArrayOutputStream().use {
																val ack = createRespondFrame(
																	connection,
																	byteArrayOf(),
																	TCPFlag.ACKNOWLEDGEMENT_NUMBER
																)
																val encap = IPFrameEncapsulated(ack)
																encap.write(it)
																DataMessage(it.toByteArray()).write(flushStream)
																flushStream.flush()
																logTCP(ack)
															}
														}
														if (ppp.frame.tcpFlags.contains(TCPFlag.LAST_PACKET)) {
															ByteArrayOutputStream().use {
																val fin = createRespondFrame(
																	connection,
																	byteArrayOf(),
																	TCPFlag.LAST_PACKET,
																	TCPFlag.ACKNOWLEDGEMENT_NUMBER
																)
																val encap = IPFrameEncapsulated(fin)
																encap.write(it)
																DataMessage(it.toByteArray()).write(flushStream)
																flushStream.flush()
																logTCP(fin)
															}
															connection.close()
														}
													} else if (connection != null) tcp.remove(ppp.frame.sourcePort)
													else {
														ByteArrayOutputStream().use {
															val fin = TCPFrame(
																0, 0, 0, listOf(IPFlag.DONT_FRAGMENT),
																0, 64, ppp.frame.destination, ppp.frame.source,
																ppp.frame.destPort, ppp.frame.sourcePort,
																ppp.frame.acknowledgementNumber, 0,
																listOf(TCPFlag.RESET_CONNECTION),
																64240, 0, 0, listOf(), byteArrayOf()
															)
															val encap = IPFrameEncapsulated(fin)
															encap.write(it)
															DataMessage(it.toByteArray()).write(flushStream)
															flushStream.flush()
															logTCP(fin)
														}
													}
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
															ByteArrayOutputStream().use {
																val udp = UDPFrame(
																	0, 0, 0, listOf(IPFlag.DONT_FRAGMENT),
																	0, 64,
																	ppp.frame.destination, ppp.frame.source,
																	ppp.frame.destPort, ppp.frame.sourcePort,
																	0, read
																)
																val encap = IPFrameEncapsulated(udp)
																encap.write(it)
																logLn(LIME, "(IP, UDP) < $udp")
																DataMessage(it.toByteArray()).write(flushStream)
															}
															seq++
														}
													} catch (_: AsynchronousCloseException) {
													}
												}
											}

											else -> TODO(ppp.frame.toString())
										}

										is LinkControlTerminationRequest ->
											TODO("LCP TERM: ${ppp.data.decodeToString()}")

										is InternetProtocolControlTerminationRequest ->
											TODO("IPCP TERM: ${ppp.data.decodeToString()}")

										else -> TODO(ppp.toString())
									}
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