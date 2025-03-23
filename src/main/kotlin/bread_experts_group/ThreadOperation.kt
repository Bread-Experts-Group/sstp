package bread_experts_group

import bread_experts_group.protocol.ipv4.InternetProtocolFrame
import bread_experts_group.protocol.ipv4.InternetProtocolFrame.IPFlag
import bread_experts_group.protocol.ipv4.icmp.ICMPDestinationUnreachable
import bread_experts_group.protocol.ipv4.icmp.ICMPEcho
import bread_experts_group.protocol.ipv4.icmp.ICMPFrame
import bread_experts_group.protocol.ipv4.tcp.TCPFrame
import bread_experts_group.protocol.ipv4.tcp.TCPFrame.TCPFlag
import bread_experts_group.protocol.ipv4.udp.UDPFrame
import bread_experts_group.protocol.ppp.PointToPointProtocolFrame
import bread_experts_group.protocol.ppp.ccp.CCPNonAcknowledgement
import bread_experts_group.protocol.ppp.ccp.CCPRequest
import bread_experts_group.protocol.ppp.ip.IPFrameEncapsulated
import bread_experts_group.protocol.ppp.ipcp.IPCPAcknowledgement
import bread_experts_group.protocol.ppp.ipcp.IPCPNonAcknowledgement
import bread_experts_group.protocol.ppp.ipcp.IPCPRequest
import bread_experts_group.protocol.ppp.ipcp.IPCPTermination
import bread_experts_group.protocol.ppp.ipcp.option.IPCPAddressOption
import bread_experts_group.protocol.ppp.ipv6cp.IPv6CPRequest
import bread_experts_group.protocol.ppp.lcp.*
import bread_experts_group.protocol.ppp.lcp.option.LCPAddressAndControlCompressionOption
import bread_experts_group.protocol.ppp.lcp.option.LCPAuthenticationProtocolOption
import bread_experts_group.protocol.ppp.lcp.option.LCPAuthenticationProtocolOption.AuthenticationProtocol
import bread_experts_group.protocol.ppp.lcp.option.LCPMagicNumberOption
import bread_experts_group.protocol.ppp.lcp.option.LCPProtocolFieldCompressionOption
import bread_experts_group.protocol.ppp.pap.PAPAcknowledge
import bread_experts_group.protocol.ppp.pap.PAPRequest
import bread_experts_group.protocol.sstp.attribute.SSTPCryptoBindingRequestAttribute
import bread_experts_group.protocol.sstp.attribute.SSTPCryptoBindingRequestAttribute.HashProtocol
import bread_experts_group.protocol.sstp.attribute.SSTPEncapsulatedProtocolAttribute.ProtocolType
import bread_experts_group.protocol.sstp.attribute.SSTPStatusAttribute
import bread_experts_group.protocol.sstp.attribute.SSTPStatusAttribute.AttributeTypeStatus
import bread_experts_group.protocol.sstp.attribute.SSTPStatusAttribute.Status
import bread_experts_group.protocol.sstp.message.*
import bread_experts_group.protocol.sstp.message.encapsulate.IPEncapsulate
import bread_experts_group.protocol.sstp.message.encapsulate.PPPEncapsulate
import bread_experts_group.util.*
import java.io.ByteArrayInputStream
import java.io.EOFException
import java.io.InputStream
import java.io.OutputStream
import java.net.*
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousCloseException
import java.nio.channels.DatagramChannel
import java.util.*
import kotlin.properties.Delegates

fun operation(
	inStream: InputStream,
	outStream: OutputStream,
	singleArgs: SingleArgs,
	multipleArgs: MultipleArgs,
	socket: Socket,
	random: Random
) {
	var state by Delegates.observable(ServerState.ServerConnectRequestPending) { _, old, new ->
		logLn(PALE_PINK, "State switch: $old -> $new")
	}
	var magicMe = 0
	var lcpThem: LCPAcknowledgement? = null
	var lcpMe: LCPAcknowledgement? = null
	var ipcpThem: IPCPAcknowledgement? = null
	var ipcpMe: IPCPAcknowledgement? = null
	var ipcpMyAddress: Inet4Address? = null
	var ipcpTheirAddress: Inet4Address? = null
	lateinit var protocol: ProtocolType

	fun handleSSTPEcho(sstp: SSTPEcho) {
		logLn(PALE_LIME, "> $sstp")
		if (sstp.type == SSTPControlMessage.MessageType.SSTP_MSG_ECHO_RESPONSE) return
		val response = SSTPEcho(false)
		response.write(outStream)
		logLn(LIME, "< $response")
	}

	fun handleLCPEcho(ppp: LCPEcho) {
		logLn(PALE_LIME, "> ${PPPEncapsulate(ppp)}")
		PPPEncapsulate(LCPEcho(ppp.identifier, magicMe, ppp.data, false))
			.also {
				it.write(outStream)
				logLn(LIME, "< $it")
			}
	}

	fun handleCCP(ppp: CCPRequest) {
		logLn(PALE_TEAL, "> ${PPPEncapsulate(ppp)}")
		if (ppp.options.isNotEmpty()) {
			PPPEncapsulate(CCPNonAcknowledgement(ppp.identifier, listOf(ppp.options.first())))
				.also {
					it.write(outStream)
					logLn(PALE_PURPLE, "< $it")
				}
		} else {
			TODO("STUFF")
		}
	}

	fun handleIPCP(ppp: IPCPRequest) {
		logLn(PALE_TEAL, "> ${PPPEncapsulate(ppp)}")
		val ip = ppp.options.firstNotNullOfOrNull { it as? IPCPAddressOption }
		if (ip == null || ip.address.address.sum() == 0) {
			PPPEncapsulate(
				IPCPNonAcknowledgement(
					listOf(
						IPCPAddressOption(
							inet4(
								0,
								random.nextInt(1, 255),
								random.nextInt(1, 255),
								random.nextInt(1, 255)
							)
						)
					),
					ppp.identifier
				)
			).also {
				it.write(outStream)
				logLn(PALE_PURPLE, "< $it")
			}
			return
		}
		ipcpTheirAddress = ip.address
		PPPEncapsulate(IPCPAcknowledgement(ppp.options, ppp.identifier)).also {
			it.write(outStream)
			ipcpThem = it.pppFrame
			logLn(TEAL, "< $it")
		}
		ipcpMyAddress = inet4(
			0,
			random.nextInt(1, 255),
			random.nextInt(1, 255),
			random.nextInt(1, 255)
		)
		PPPEncapsulate(
			IPCPRequest(
				listOf(IPCPAddressOption(ipcpMyAddress!!)),
				ppp.identifier + 1
			)
		).also {
			it.write(outStream)
			logLn(PALE_TEAL, "< $it")
		}
	}

	fun handleIPCPTermination(ppp: IPCPTermination?) {
		if (ppp != null) {
			logLn(PALE_RED, "> ${PPPEncapsulate(ppp)}")
			PPPEncapsulate(IPCPTermination(ppp.identifier, byteArrayOf(), false)).also {
				it.write(outStream)
				logLn(LIGHT_PINK, "< $it")
			}
		}
		ipcpMyAddress = null
		ipcpMe = null
		ipcpTheirAddress = null
		ipcpThem = null
	}

	fun handleLCPTermination(ppp: LCPTermination) {
		logLn(PALE_RED, "> ${PPPEncapsulate(ppp)}")
		PPPEncapsulate(LCPTermination(ppp.identifier, byteArrayOf(), false)).also {
			it.write(outStream)
			logLn(LIGHT_PINK, "< $it")
		}
		handleIPCPTermination(null)
		lcpThem = null
		lcpMe = null
		magicMe = 0
	}

	fun sendICMPUnreachable(code: Int, frame: InternetProtocolFrame) = IPEncapsulate(
		ICMPDestinationUnreachable(
			0, 0, 0, listOf(IPFlag.DONT_FRAGMENT),
			0, 64, frame.destination, frame.source,
			frame.asBytes(), code
		)
	).also {
		it.write(outStream)
		logLn(PALE_PINKISH_RED, "< $it")
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
		fun abort(status: Status) {
			val abort = SSTPAbort(
				SSTPStatusAttribute(
					AttributeTypeStatus.SSTP_ATTRIB_NO_ERROR,
					status
				)
			)
			abort.write(outStream)
			logLn(GRAY, "< $abort")
		}

		try {
			when (state) {
				ServerState.ServerConnectRequestPending -> when (val message = SSTPMessage.read(inStream)) {
					is SSTPConnectionRequest -> {
						logLn(GRAY, "> $message")
						protocol = message.attribute.protocolID
						state = ServerState.ServerCallConnectedPending
						val ack = SSTPConnectionAcknowledge(
							SSTPCryptoBindingRequestAttribute(
								listOf(HashProtocol.CERT_HASH_PROTOCOL_SHA256),
								"abcdefghabcdefghabcdefghabcdefgh".encodeToByteArray()
							)
						)
						ack.write(outStream)
						logLn(GRAY, "< $ack")
					}

					else -> {
						logLn(GRAY, "> $message")
						abort(Status.ATTRIB_STATUS_UNACCEPTED_FRAME_RECEIVED)
						break
					}
				}

				ServerState.ServerCallConnectedPending -> when (val message = SSTPMessage.read(inStream)) {
					is SSTPDataMessage -> when (protocol) {
						ProtocolType.SSTP_ENCAPSULATED_PROTOCOL_PPP -> {
							val ppp = PointToPointProtocolFrame.read(ByteArrayInputStream(message.data))
							if (lcpThem == null) {
								ppp as LCPRequest
								logLn(PALE_ORANGE, "> ${PPPEncapsulate(ppp)}")
								var a = ppp.options.firstOrNull { it is LCPProtocolFieldCompressionOption }
								var b = ppp.options.firstOrNull { it is LCPAddressAndControlCompressionOption }
								if (a != null || b != null) {
									PPPEncapsulate(
										LCPNonAcknowledgement(
											buildList {
												if (a != null) add(a)
												if (b != null) add(b)
											},
											ppp.identifier
										)
									).also {
										it.write(outStream)
										logLn(PALE_RED, "< $it")
									}
								} else {
									PPPEncapsulate(LCPAcknowledgement(ppp.options, ppp.identifier)).also {
										it.write(outStream)
										lcpThem = it.pppFrame
										logLn(ORANGE, "< $it")
									}
									magicMe = random.nextInt()
									PPPEncapsulate(
										LCPRequest(
											buildList {
												add(LCPMagicNumberOption(magicMe))
												if (!multipleArgs[Flags.PAP_USERNAME].isNullOrEmpty())
													add(
														LCPAuthenticationProtocolOption(
															AuthenticationProtocol.PASSWORD_AUTHENTICATION_PROTOCOL
														)
													)
											},
											0x00
										)
									).also {
										it.write(outStream)
										logLn(PALE_BLUE, "< $it")
									}
								}
							} else if (lcpMe == null) {
								if (ppp is LCPAcknowledgement) {
									logLn(BLUE, "> ${PPPEncapsulate(ppp)}")
									lcpMe = ppp
								} else TODO(ppp.gist())
							} else when (ppp) {
								is LCPEcho -> handleLCPEcho(ppp)
								is CCPRequest -> TODO("Reject") //handleCCP(ppp)
								is IPCPRequest -> handleIPCP(ppp)
								is IPv6CPRequest -> TODO("Reject")
								is PAPRequest -> {
									logLn(PALE_PINKISH_RED, "> ${PPPEncapsulate(ppp)}")
									val idIndex = multipleArgs.getValue(Flags.PAP_USERNAME).indexOf(ppp.peerID)
									val passphrase = multipleArgs.getValue(Flags.PAP_PASSPHRASE).getOrNull(idIndex)
									if (idIndex == -1 || (passphrase != null && ppp.password != passphrase)) {
										val err = (singleArgs.getValue(Flags.AUTHENTICATION_FAILURE_MESSAGE) as String)
											.format(ppp.peerID)
										PPPEncapsulate(PAPAcknowledge(ppp.identifier, err, false)).also {
											it.write(outStream)
											logLn(PALE_PINKISH_RED, "< $it")
										}
									} else {
										val ok = (singleArgs.getValue(Flags.AUTHENTICATION_SUCCESSFUL_MESSAGE) as String)
											.format(ppp.peerID)
										PPPEncapsulate(PAPAcknowledge(ppp.identifier, ok, true)).also {
											it.write(outStream)
											logLn(LIGHT_PINK, "< $it")
										}
									}
								}

								is LCPTermination -> handleLCPTermination(ppp)
								else -> TODO(ppp.gist())
							}
						}
					}

					is SSTPConnected -> {
						// TODO: Confirm crypto binding to avoid MITM
						state = ServerState.ServerCallConnected
					}

					else -> {
						logLn(GRAY, "> $message")
						abort(Status.ATTRIB_STATUS_UNACCEPTED_FRAME_RECEIVED)
						break
					}
				}

				ServerState.ServerCallConnected -> when (val message = SSTPMessage.read(inStream)) {
					is SSTPDataMessage -> when (protocol) {
						ProtocolType.SSTP_ENCAPSULATED_PROTOCOL_PPP -> {
							val ppp = PointToPointProtocolFrame.read(ByteArrayInputStream(message.data))
							when (ppp) {
								is LCPEcho -> handleLCPEcho(ppp)
								is CCPRequest -> handleCCP(ppp)
								is IPCPRequest -> handleIPCP(ppp)
								is IPCPAcknowledgement -> {
									logLn(TEAL, "> ${PPPEncapsulate(ppp)}")
									ipcpMe = ppp
								}

								is IPFrameEncapsulated -> {
									val actualDestination =
										if (ppp.frame.destination == ipcpMyAddress!!) InetAddress.getLoopbackAddress()
										else if (ppp.frame.destination == ipcpTheirAddress!!) socket.localAddress
										else ppp.frame.destination
									when (ppp.frame) {
										is ICMPFrame -> when (ppp.frame) {
											is ICMPEcho -> {
												if (ppp.frame.type == ICMPFrame.ICMPType.ECHO_REPLY) {
													logLn(PALE_PINK, "> ${PPPEncapsulate(ppp)}")
													val actualReq = actualDestination.isReachable(1000)
													if (actualReq) IPEncapsulate(
														ICMPEcho(
															0, 0, 0, listOf(IPFlag.DONT_FRAGMENT),
															0, 64, ppp.frame.destination, ppp.frame.source,
															ppp.frame.echoIdentifier, ppp.frame.echoSequence,
															ppp.frame.data, false
														)
													).also {
														it.write(outStream)
														logLn(PALE_PINKISH_RED, "< $it")
													} else sendICMPUnreachable(1, ppp.frame)
												}
											}

											else -> null
										}

										is TCPFrame -> {
											fun sendTCP(frame: TCPFrame) = IPEncapsulate(frame).let {
												it.write(outStream)
												logLn(PALE_PINK, "< $it")
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
													64240, 0, 0, emptyList(),
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
														64240, 0, 0, emptyList(),
														byteArrayOf()
													)
												)
												tcp.remove(ppp.frame.sourcePort)
											}

											logLn(PALE_PINKISH_RED, "> ${PPPEncapsulate(ppp)}")
											val connection = tcp[ppp.frame.sourcePort]
											if (connection == null) {
												if (ppp.frame.flags.size == 1 && ppp.frame.tcpFlags[0] == TCPFlag.SYN) {
													try {
														val socket = Socket()
														socket.connect(
															InetSocketAddress(
																actualDestination,
																ppp.frame.destPort
															),
															1000
														)
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
														newConnection.seq = random.nextInt()
														sendFrame(newConnection, TCPFlag.SYN, TCPFlag.ACK)
														newConnection.seq++
													} catch (_: ConnectException) {
														sendICMPUnreachable(3, ppp.frame)
													} catch (_: SocketTimeoutException) {
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
												if (send && flags.isNotEmpty())
													sendFrame(connection, *flags.toTypedArray())
											}
										}

										is UDPFrame -> {
											logLn(PALE_LIME, "> ${PPPEncapsulate(ppp)}")
											Thread.ofPlatform().start {
												val channel = DatagramChannel.open()
												channel.connect(
													InetSocketAddress(
														actualDestination,
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
															it.write(outStream)
															logLn(LIME, "< $it")
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

										else -> TODO(ppp.frame.gist())
									}
								}

								is LCPTermination -> handleLCPTermination(ppp)
								is IPCPTermination -> handleIPCPTermination(ppp)
								else -> TODO(ppp.gist())
							}
						}
					}

					is SSTPEcho -> handleSSTPEcho(message)

					else -> {
						logLn(GRAY, "> $message")
						abort(Status.ATTRIB_STATUS_UNACCEPTED_FRAME_RECEIVED)
						break
					}
				}
			}
		} catch (e: Exception) {
			when (e) {
				is EOFException -> logLn(PALE_RED, "Session ended (client disconnect).")
				is SocketException -> logLn(PALE_RED, "Session ended (socket error/server disconnect); ${e.message}.")
				else -> {
					try {
						abort(Status.ATTRIB_STATUS_NO_ERROR)
					} catch (w: Exception) {
						when (w) {
							is EOFException -> logLn("Failed to write abort; client unavailable.")
							else -> logLn("Failed to write abort; ${e.message}")
						}
					}
					logLn("Session ended (server failure); ${e.message}")
				}
			}
			break
		}
	}
	socket.close()
}