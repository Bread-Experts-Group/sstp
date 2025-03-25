package bread_experts_group

import bread_experts_group.protocol.ip.tcp.TCPFrame
import bread_experts_group.protocol.ip.tcp.TCPFrame.TCPFlag
import bread_experts_group.protocol.ip.udp.UDPFrame
import bread_experts_group.protocol.ip.v4.InternetProtocolFrame
import bread_experts_group.protocol.ip.v4.InternetProtocolFrame.IPFlag
import bread_experts_group.protocol.ip.v4.icmp.ICMPDestinationUnreachable
import bread_experts_group.protocol.ip.v4.icmp.ICMPEcho
import bread_experts_group.protocol.ip.v4.icmp.ICMPFrame
import bread_experts_group.protocol.ip.v6.icmp.ICMPV6Frame
import bread_experts_group.protocol.ppp.PointToPointProtocolFrame
import bread_experts_group.protocol.ppp.ccp.CCPNonAcknowledgement
import bread_experts_group.protocol.ppp.ccp.CCPRequest
import bread_experts_group.protocol.ppp.ip.IPFrameEncapsulated
import bread_experts_group.protocol.ppp.ip.IPv6FrameEncapsulated
import bread_experts_group.protocol.ppp.ipcp.IPCPAcknowledgement
import bread_experts_group.protocol.ppp.ipcp.IPCPNonAcknowledgement
import bread_experts_group.protocol.ppp.ipcp.IPCPRequest
import bread_experts_group.protocol.ppp.ipcp.IPCPTermination
import bread_experts_group.protocol.ppp.ipcp.option.IPCPAddressOption
import bread_experts_group.protocol.ppp.ipv6cp.IPv6CPAcknowledgement
import bread_experts_group.protocol.ppp.ipv6cp.IPv6CPNonAcknowledgement
import bread_experts_group.protocol.ppp.ipv6cp.IPv6CPRequest
import bread_experts_group.protocol.ppp.ipv6cp.option.IPv6CPInterfaceIdentifierOption
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
	tlsSocket: Socket,
	random: Random
) {
	var state by Delegates.observable(ServerState.ServerConnectRequestPending) { _, old, new ->
		logLn(PALE_PINK, "State switch: $old -> $new")
	}
	var magicMe = 0
	var lcpThem: LCPAcknowledgement? = null
	var lcpMe: LCPAcknowledgement? = null
	var ipcpMyAddress: Inet4Address? = null
	var ipcpTheirAddress: Inet4Address? = null
	var ipv6cpMyInterface: ByteArray? = null
	var ipv6cpTheirInterface: ByteArray? = null
	var isAuthorized = 0
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

	fun handleIPCPTermination(ppp: IPCPTermination?, data: ByteArray) {
		if (ppp != null) {
			logLn(PALE_RED, "> ${PPPEncapsulate(ppp)}")
			PPPEncapsulate(IPCPTermination(ppp.identifier, data, false)).also {
				it.write(outStream)
				logLn(LIGHT_PINK, "< $it")
			}
		} else {
			PPPEncapsulate(IPCPTermination(0, data, true)).also {
				it.write(outStream)
				logLn(LIGHT_PINK, "< $it")
			}
		}
		ipcpMyAddress = null
		ipcpTheirAddress = null
	}

	fun handleLCPTermination(ppp: LCPTermination?, data: ByteArray) {
		if (ppp != null) {
			logLn(PALE_RED, "> ${PPPEncapsulate(ppp)}")
			PPPEncapsulate(LCPTermination(ppp.identifier, data, false)).also {
				it.write(outStream)
				logLn(LIGHT_PINK, "< $it")
			}
		} else {
			PPPEncapsulate(LCPTermination(0, data, true)).also {
				it.write(outStream)
				logLn(LIGHT_PINK, "< $it")
			}
		}
		lcpThem = null
		lcpMe = null
		magicMe = 0
	}

	fun checkAuthForNCP() {
		if (isAuthorized != -1) handleLCPTermination(null, "Failure to authenticate".encodeToByteArray())
	}

	fun handleIPCP(ppp: IPCPRequest) {
		logLn(PALE_TEAL, "> ${PPPEncapsulate(ppp)}")
		val addressOpt = ppp.options.firstNotNullOf { it as? IPCPAddressOption }
		if (addressOpt.address.address.sumOf { it.toInt() } == 0) {
			val modOpts = ppp.options.toMutableList()
			modOpts.remove(addressOpt)
			val assigned = Inet4Address.getByName(singleArgs.getValue(Flags.VPN_REMOTE_ADDRESS_V4) as String)
			modOpts.add(IPCPAddressOption(assigned as Inet4Address))
			PPPEncapsulate(IPCPNonAcknowledgement(modOpts, ppp.identifier)).also {
				it.write(outStream)
				logLn(TEAL, "< $it")
			}
		} else {
			PPPEncapsulate(IPCPAcknowledgement(ppp.options, ppp.identifier)).also {
				it.write(outStream)
				ipcpTheirAddress = addressOpt.address
				logLn(TEAL, "< $it")
			}
		}
	}

	val llAddr = Inet4Address.getByName(singleArgs.getValue(Flags.VPN_LOCAL_ADDRESS_V4) as String) as Inet4Address
	fun requestIPCP(ppp: IPCPNonAcknowledgement?) {
		val address = if (ppp != null) {
			logLn(PALE_PURPLE, "> ${PPPEncapsulate(ppp)}")
			val setAddr = ppp.options.firstNotNullOf { it as? IPCPAddressOption }
			if (setAddr.address.address.sumOf { it.toInt() } == 0) llAddr
			else setAddr.address
		} else llAddr
		PPPEncapsulate(
			IPCPRequest(
				listOf(IPCPAddressOption(address)),
				0
			)
		).also {
			it.write(outStream)
			logLn(PALE_TEAL, "< $it")
		}
	}

	fun handleIPv6CP(ppp: IPv6CPRequest) {
		logLn(PALE_TEAL, "> ${PPPEncapsulate(ppp)}")
		val interfaceID = ppp.options.firstNotNullOf { it as? IPv6CPInterfaceIdentifierOption }
		if (interfaceID.identifier.sumOf { it.toInt() } == 0) {
			val modOpts = ppp.options.toMutableList()
			modOpts.remove(interfaceID)
			val assigned = Inet6Address.getByName(singleArgs.getValue(Flags.VPN_REMOTE_ADDRESS_V6) as String)
			modOpts.add(IPv6CPInterfaceIdentifierOption(assigned.address.sliceArray(8..15)))
			PPPEncapsulate(IPv6CPNonAcknowledgement(modOpts, ppp.identifier)).also {
				it.write(outStream)
				logLn(TEAL, "< $it")
			}
		} else {
			PPPEncapsulate(IPv6CPAcknowledgement(ppp.options, ppp.identifier)).also {
				it.write(outStream)
				ipv6cpTheirInterface = interfaceID.identifier
				logLn(TEAL, "< $it")
			}
		}
	}

	fun requestIPv6CP(ppp: IPv6CPNonAcknowledgement?) {
		val id = if (ppp != null) {
			logLn(PALE_PURPLE, "> ${PPPEncapsulate(ppp)}")
			val setID = ppp.options.firstNotNullOf { it as? IPv6CPInterfaceIdentifierOption }
			if (setID.identifier.sumOf { it.toInt() } == 0) ByteArray(8).also { random.nextBytes(it) }
			else setID.identifier
		} else ByteArray(8)
		PPPEncapsulate(
			IPv6CPRequest(
				listOf(IPv6CPInterfaceIdentifierOption(id)),
				0
			)
		).also {
			it.write(outStream)
			logLn(PALE_TEAL, "< $it")
		}
	}

//	fun handleCCP(ppp: CCPRequest) {
//		logLn(PALE_ORANGE, "> ${PPPEncapsulate(ppp)}")
//		val deflate = ppp.options.firstNotNullOfOrNull { it as? CCPDEFLATEOption }
//		if (deflate != null && deflate.windowSize == 32768) PPPEncapsulate(
//			CCPAcknowledgement(ppp.identifier, listOf(deflate))
//		).also {
//			it.write(outStream)
//			logLn(PALE_ORANGE, "< $it")
//		} else PPPEncapsulate(
//			CCPNonAcknowledgement(ppp.identifier, ppp.options)
//		).also {
//			it.write(outStream)
//			logLn(PALE_RED, "< $it")
//		}
//	}
//
//	fun requestCCP(id: Int) {
//		PPPEncapsulate(
//			CCPRequest(
//				id,
//				listOf(CCPDEFLATEOption(32768, DEFLATEMethod.DEFLATE, DEFLATECheckMethod.SEQUENCE))
//			)
//		).also {
//			it.write(outStream)
//			logLn(PALE_RED, "< $it")
//		}
//	}

	fun handleNCP(ppp: PointToPointProtocolFrame) {
		checkAuthForNCP()
		when (ppp) {
			is CCPRequest -> {
				// TODO: Stabilize CCP
				logLn(PALE_RED, " > ${PPPEncapsulate(ppp)}")
				PPPEncapsulate(
					CCPNonAcknowledgement(
						ppp.identifier, ppp.options
					)
				).also {
					it.write(outStream)
					logLn(PALE_RED, "< $it")
				}
//				handleCCP(ppp)
//				requestCCP(ppp.identifier + 1)
			}

//			is CCPAcknowledgement -> {
//				logLn(PALE_ORANGE, "> ${PPPEncapsulate(ppp)}")
//				PointToPointProtocolFrame.compressionHandler = {
//					it.read16() // sequence
//					DEFLATEStream(it)
//				}
//			}

			// IPv4 Configuration Protocol
			is IPCPAcknowledgement -> {
				logLn(TEAL, "> ${PPPEncapsulate(ppp)}")
				ipcpMyAddress = ppp.options.firstNotNullOf { it as? IPCPAddressOption }.address
			}

			is IPCPNonAcknowledgement -> requestIPCP(ppp)
			is IPCPRequest -> {
				handleIPCP(ppp)
				if (ipcpMyAddress == null) requestIPCP(null)
			}

			// IPv6 Configuration Protocol
			is IPv6CPAcknowledgement -> {
				logLn(TEAL, " > ${PPPEncapsulate(ppp)}")
				ipv6cpMyInterface = ppp.options.firstNotNullOf {
					it as? IPv6CPInterfaceIdentifierOption
				}.identifier
			}

			is IPv6CPNonAcknowledgement -> requestIPv6CP(ppp)
			is IPv6CPRequest -> {
				handleIPv6CP(ppp)
				if (ipv6cpMyInterface == null) requestIPv6CP(null)
			}

			else -> TODO(ppp.gist())
		}
	}

	fun sendICMPUnreachable(code: Int, frame: InternetProtocolFrame<*>) = IPEncapsulate(
		InternetProtocolFrame(
			0, 0, 0, listOf(IPFlag.DONT_FRAGMENT),
			0, 64, frame.destination, frame.source,
			ICMPDestinationUnreachable(frame.asBytes(), code)
		)
	).also {
		it.write(outStream)
		logLn(PALE_RED, "< $it")
	}

	data class TCPConnection(
		val socket: Socket,
		val sendBuffer: MutableList<ByteArray> = mutableListOf(),
		var ending: Boolean = false,
		var ack: Int = -1,
		var lastAck: Int = -1,
		var seq: Int = -1
	) {
		var removeEntry: Boolean = false
			private set

		fun close() {
			this.socket.close()
			this.sendBuffer.clear()
			this.removeEntry = true
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
			logLn(GRAY, "< ${abort.gist()}")
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
								PPPEncapsulate(LCPAcknowledgement(ppp.options, ppp.identifier)).also {
									PointToPointProtocolFrame.compression.inbound.protocol = ppp.options.any {
										it is LCPProtocolFieldCompressionOption
									}
									PointToPointProtocolFrame.compression.inbound.addressAndControl = ppp.options.any {
										it is LCPAddressAndControlCompressionOption
									}
									it.write(outStream)
									lcpThem = it.pppFrame
									logLn(ORANGE, "< $it")
								}
								magicMe = random.nextInt()
								PPPEncapsulate(
									LCPRequest(
										buildList {
											add(LCPMagicNumberOption(magicMe))
											add(LCPProtocolFieldCompressionOption())
											add(LCPAddressAndControlCompressionOption())
											if (!multipleArgs[Flags.PAP_USERNAME].isNullOrEmpty()) {
												add(
													LCPAuthenticationProtocolOption(
														AuthenticationProtocol.PASSWORD_AUTHENTICATION_PROTOCOL
													)
												)
											} else isAuthorized = -1
										},
										0x00
									)
								).also {
									it.write(outStream)
									logLn(PALE_BLUE, "< $it")
								}
							} else if (lcpMe == null) {
								if (ppp is LCPAcknowledgement) {
									PointToPointProtocolFrame.compression.outbound.protocol = ppp.options.any {
										it is LCPProtocolFieldCompressionOption
									}
									PointToPointProtocolFrame.compression.outbound.addressAndControl = ppp.options.any {
										it is LCPAddressAndControlCompressionOption
									}
									logLn(BLUE, "> ${PPPEncapsulate(ppp)}")
									lcpMe = ppp
								} else TODO(ppp.gist())
							} else when (ppp) {
								is LCPEcho -> handleLCPEcho(ppp)

								is PAPRequest -> {
									logLn(PALE_RED, "> ${PPPEncapsulate(ppp).gist()}")
									val idIndex = multipleArgs.getValue(Flags.PAP_USERNAME).indexOf(ppp.peerID)
									val passphrase = multipleArgs.getValue(Flags.PAP_PASSPHRASE).getOrNull(idIndex)
									if (idIndex == -1 || (passphrase != null && ppp.password != passphrase)) {
										val err = (singleArgs.getValue(Flags.AUTHENTICATION_FAILURE_MESSAGE) as String)
											.format(ppp.peerID)
										PPPEncapsulate(PAPAcknowledge(ppp.identifier, err, false)).also {
											it.write(outStream)
											logLn(PALE_RED, "< ${it.gist()}")
										}
										isAuthorized++
										if (isAuthorized > singleArgs.getValue(Flags.AUTHENTICATION_TRIES) as Int)
											handleLCPTermination(
												null,
												"Failure to authenticate ${isAuthorized - 1} times".encodeToByteArray()
											)
									} else {
										val ok = (singleArgs.getValue(Flags.AUTHENTICATION_SUCCESSFUL_MESSAGE) as String)
											.format(ppp.peerID)
										PPPEncapsulate(PAPAcknowledge(ppp.identifier, ok, true)).also {
											it.write(outStream)
											logLn(LIGHT_PINK, "< ${it.gist()}")
										}
										isAuthorized = -1
									}
								}

								is LCPTermination -> handleLCPTermination(ppp, ppp.data)
								else -> handleNCP(ppp)
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

								is IPv6FrameEncapsulated -> {
									when (ppp.frame.data) {
										is ICMPV6Frame -> logLn(" > ${PPPEncapsulate(ppp)}")
										else -> TODO(ppp.frame.gist())
									}
								}

								is IPFrameEncapsulated -> {
									val actualDestination =
										if (ppp.frame.destination == ipcpMyAddress!!) InetAddress.getLoopbackAddress()
										else if (ppp.frame.destination == ipcpTheirAddress!!) tlsSocket.localAddress
										else ppp.frame.destination
									when (ppp.frame.data) {
										is ICMPEcho -> {
											if (ppp.frame.data.type == ICMPFrame.ICMPType.ECHO_REQUEST) {
												logLn(PALE_PINK, "> ${PPPEncapsulate(ppp)}")
												val actualReq = actualDestination.isReachable(
													singleArgs.getValue(Flags.ICMP_TIMEOUT) as Int
												)
												if (actualReq) IPEncapsulate(
													InternetProtocolFrame(
														0, 0, 0, listOf(IPFlag.DONT_FRAGMENT),
														0, 64, ppp.frame.destination, ppp.frame.source,
														ICMPEcho(
															ppp.frame.data.echoIdentifier, ppp.frame.data.echoSequence,
															ppp.frame.data.data, false
														)
													)
												).also {
													it.write(outStream)
													logLn(PALE_RED, "< $it")
												} else sendICMPUnreachable(1, ppp.frame)
											}
										}

										is TCPFrame -> {
											fun sendTCP(frame: TCPFrame) = IPEncapsulate(
												InternetProtocolFrame(
													0, 0, 0, listOf(IPFlag.DONT_FRAGMENT),
													0, 64, ppp.frame.destination, ppp.frame.source,
													frame
												)
											).let {
												it.write(outStream)
												logLn(PALE_PINK, "< $it")
											}

											fun sendFrame(
												connection: TCPConnection,
												vararg flag: TCPFlag,
												data: ByteArray = byteArrayOf()
											) {
												sendTCP(
													TCPFrame(
														ppp.frame.data.destPort, ppp.frame.data.sourcePort,
														connection.seq, connection.ack,
														listOf(*flag),
														64240, 0, 0, emptyList(),
														data
													)
												)
												connection.seq += data.size
											}

											fun sendReset() {
												sendTCP(
													TCPFrame(
														ppp.frame.data.destPort, ppp.frame.data.sourcePort,
														ppp.frame.data.acknowledgementNumber, ppp.frame.data.sequence,
														listOf(TCPFlag.RST),
														64240, 0, 0, emptyList(),
														byteArrayOf()
													)
												)
												tcp.remove(ppp.frame.data.sourcePort)
											}

											logLn(PALE_RED, "> ${PPPEncapsulate(ppp)}")
											if (ppp.frame.data.tcpFlags.contains(TCPFlag.RST)) {
												tcp.remove(ppp.frame.data.sourcePort)
												continue
											}

											val connection = tcp[ppp.frame.data.sourcePort]
											if (connection == null) {
												if (ppp.frame.flags.size == 1 && ppp.frame.data.tcpFlags[0] == TCPFlag.SYN) {
													try {
														val socket = Socket()
														socket.connect(
															InetSocketAddress(
																actualDestination,
																ppp.frame.data.destPort
															),
															singleArgs.getValue(Flags.TCP_TIMEOUT) as Int
														)
														val newConnection = TCPConnection(socket)
														Thread.ofVirtual().start {
															val buffer = ByteArray(40)
															try {
																while (true) {
																	val readCount = socket.inputStream.read(buffer)
																	if (readCount == -1) break
																	synchronized(newConnection) {
																		val send = buffer.sliceArray(0 until readCount)
																		sendFrame(
																			newConnection, TCPFlag.ACK, TCPFlag.PSH,
																			data = send
																		)
																	}
																}
															} catch (_: SocketException) {
															}
														}
														tcp[ppp.frame.data.sourcePort] = newConnection
														newConnection.ack = ppp.frame.data.sequence + 1
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
											} else synchronized(connection) {
												var send = true
												var data = byteArrayOf()
												val flags = mutableListOf<TCPFlag>()
												if (
													ppp.frame.data.tcpFlags.contains(TCPFlag.ACK) &&
													!ppp.frame.data.tcpFlags.contains(TCPFlag.SYN)
												) {
													if (ppp.frame.data.tcpData.isNotEmpty()) {
														connection.ack += ppp.frame.data.tcpData.size
														connection.sendBuffer.add(ppp.frame.data.tcpData)
														flags.add(TCPFlag.ACK)
													}
													if (connection.removeEntry) tcp.remove(ppp.frame.data.sourcePort)
												} else {
													sendReset()
													send = false
												}
												if (ppp.frame.data.tcpFlags.contains(TCPFlag.PSH)) {
													try {
														connection.sendBuffer.removeIf {
															connection.socket.outputStream.write(it)
															true
														}
													} catch (_: SocketException) {
														sendReset()
														send = false
													}
												}
												if (ppp.frame.data.tcpFlags.contains(TCPFlag.FIN)) {
													connection.ack += 1
													flags.add(TCPFlag.ACK)
													flags.add(TCPFlag.FIN)
													connection.close()
												}
												if (send && flags.isNotEmpty()) sendFrame(
													connection,
													*flags.toTypedArray(),
													data = data
												)
											}
										}

										is UDPFrame -> {
											logLn(PALE_LIME, "> ${PPPEncapsulate(ppp)}")
											Thread.ofPlatform().start {
												val channel = DatagramChannel.open()
												channel.connect(
													InetSocketAddress(
														actualDestination,
														ppp.frame.data.destPort
													)
												)
												channel.write(ByteBuffer.wrap(ppp.frame.data.udpData))
												var seq = 0
												try {
													while (true) {
														Thread.ofVirtual().start {
															val savSeq = seq
															Thread.sleep((singleArgs.getValue(Flags.UDP_TIMEOUT) as Int).toLong())
															@Suppress("KotlinConstantConditions")
															if (savSeq == seq) channel.close()
														}
														val packet = ByteBuffer.allocate(65535)
														val read = ByteArray(channel.read(packet))
														packet.flip()
														packet.get(read)
														IPEncapsulate(
															InternetProtocolFrame(

																0, 0, 0, listOf(IPFlag.DONT_FRAGMENT),
																0, 64,
																ppp.frame.destination, ppp.frame.source,
																UDPFrame(
																	ppp.frame.data.destPort, ppp.frame.data.sourcePort,
																	0, read
																)
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

								is LCPTermination -> handleLCPTermination(ppp, ppp.data)
								is IPCPTermination -> handleIPCPTermination(ppp, ppp.data)
								is LCPDiscardRequest -> {}
								is LCPProtocolRejection -> {}
								else -> handleNCP(ppp)
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
				is SocketException -> logLn(PALE_RED, "Session ended (socket error/server disconnect); ${e.stackTraceToString()}")
				else -> {
					try {
						abort(Status.ATTRIB_STATUS_NO_ERROR)
					} catch (w: Exception) {
						when (w) {
							is EOFException -> logLn(PALE_RED, "Failed to write abort; client unavailable.")
							else -> logLn(PALE_RED, "Failed to write abort; ${e.stackTraceToString()}")
						}
					}
					logLn(PALE_RED, "Session ended (server failure); ${e.stackTraceToString()}")
				}
			}
			break
		}
	}
	tlsSocket.close()
}