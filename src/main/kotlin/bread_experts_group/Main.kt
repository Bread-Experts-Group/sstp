package bread_experts_group

import bread_experts_group.util.*
import java.io.*
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.security.KeyStore
import java.security.SecureRandom
import java.security.Security
import java.util.*
import javax.net.ssl.*
import kotlin.system.exitProcess

enum class ServerState {
	ServerConnectRequestPending,
	ServerCallConnectedPending,
	ServerCallConnected
}

@OptIn(ExperimentalUnsignedTypes::class)
fun main(args: Array<String>) {
	logLn("Supplied Command Line Arguments")
	logLn("-------------------------------")
	val (singleArgs, multipleArgs) = readArgs(args)
	logLn("===============================")
	verbosity = singleArgs.getValue(Flags.VERBOSITY) as Int
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
//	logLn("Doing DHCP test")
//	logLn("-------------------------------")
//	val socket = MulticastSocket(68)
//	socket.broadcast = true
//	val hdwr = byteArrayOf(
//		0x00, 0x05, 0x3C, 0x04,
//		(0x8D).toByte(), 0x59, 0x00, 0x00,
//		0x00, 0x00, 0x00, 0x00,
//		0x00, 0x00, 0x00, 0x00
//	)
//	ByteArrayOutputStream().use {
//		val discovery = DynamicHostConfigurationProtocolFrame(
//			DHCPOperationType.BOOTREQUEST,
//			DHCPHardwareType.ETHERNET_10MB,
//			6,
//			0,
//			Random().nextInt(),
//			0,
//			listOf(DHCPFlag.BROADCAST),
//			inet4(0, 0, 0, 0),
//			inet4(0, 0, 0, 0),
//			inet4(0, 0, 0, 0),
//			inet4(0, 0, 0, 0),
//			hdwr,
//			listOf(DHCPMessageType(DHCPMessageTypes.DHCPDISCOVER))
//		)
//		discovery.write(it)
//		logLn("< $discovery")
//		val send = DatagramPacket(it.toByteArray(), it.size(), inet4(192, 168, 0, 255), 67)
//		socket.send(send)
//	}
//	val offer = ByteArray(512).let {
//		val receive = DatagramPacket(it, it.size)
//		socket.receive(receive)
//		val data = DynamicHostConfigurationProtocolFrame.read(ByteArrayInputStream(it))
//		logLn("> $data")
//		data
//	}
//	ByteArrayOutputStream().use {
//		val request = DynamicHostConfigurationProtocolFrame(
//			DHCPOperationType.BOOTREQUEST,
//			DHCPHardwareType.ETHERNET_10MB,
//			6,
//			0,
//			Random().nextInt(),
//			0,
//			listOf(DHCPFlag.BROADCAST),
//			inet4(0, 0, 0, 0),
//			inet4(0, 0, 0, 0),
//			inet4(0, 0, 0, 0),
//			inet4(0, 0, 0, 0),
//			hdwr,
//			listOf(
//				DHCPMessageType(DHCPMessageTypes.DHCPREQUEST),
//				DHCPServerAddress(offer.serverIP),
//				DHCPRequestAddress(offer.yourIP),
//				DHCPHostName("BREADXPERTSGRP")
//			)
//		)
//		request.write(it)
//		logLn("< $request")
//		val send = DatagramPacket(it.toByteArray(), it.size(), inet4(192, 168, 0, 255), 67)
//		socket.send(send)
//	}
//	ByteArray(512).let {
//		val receive = DatagramPacket(it, it.size)
//		socket.receive(receive)
//		val data = DynamicHostConfigurationProtocolFrame.read(ByteArrayInputStream(it))
//		logLn("> $data")
//		data
//	}
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
	logLn("Secure Random Algorithms: [${Security.getProperty("securerandom.strongAlgorithms")}]")
	val algorithm = singleArgs[Flags.SECURE_RANDOM_GENERATOR] as String?
	if (algorithm == null)
		logLn("You may select an algorithm with the -random flag. For a non-CSPRNG (not recommended), use an argument of NONE.")
	else if (algorithm == "NONE") logLn("Using potentially unsecure random source.")
	else {
		val rand = SecureRandom.getInstance(
			algorithm.substringBefore(':'),
			algorithm.substringAfter(':')
		)
		logLn("Using random source \"${rand.algorithm}\" (provider: ${rand.provider}, deprecated: ${rand.isDeprecated})")
		logLn("Parameters: [${rand.parameters}]")
	}
	while (true) {
		val random =
			if (algorithm == "NONE") Random()
			else if (algorithm != null) SecureRandom.getInstance(
				algorithm.substringBefore(':'),
				algorithm.substringAfter(':')
			)
			else SecureRandom.getInstanceStrong()
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
			operation(
				object : InputStream() {
					override fun read(): Int {
						val read = newSocket.inputStream.read()
						if (read == -1) throw EOFException()
						return read
					}
				},
				object : OutputStream() {
					val buffer = mutableListOf<Byte>()
					override fun write(b: Int): Unit = synchronized(buffer) {
						buffer.add(b.toByte())
					}

					override fun flush(): Unit = synchronized(buffer) {
						if (buffer.isEmpty()) throw IllegalStateException("Empty buffer")
						newSocket.outputStream.write(buffer.toByteArray())
						buffer.clear()
					}
				},
				singleArgs,
				multipleArgs,
				newSocket,
				random
			)
		}.uncaughtExceptionHandler = Thread.UncaughtExceptionHandler { thread, e ->
			when (e) {
				is SSLException -> logLn(PALE_PINKISH_RED, "TLS/SSL connection failure; ${e.stackTraceToString()}")
				else -> logLn(PALE_RED, "Server failure outside of operation; ${e.stackTraceToString()}")
			}
			newSocket.close()
		}
	}
}