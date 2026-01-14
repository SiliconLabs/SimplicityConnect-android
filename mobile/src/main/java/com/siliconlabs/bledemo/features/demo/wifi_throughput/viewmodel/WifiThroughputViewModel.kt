package com.siliconlabs.bledemo.features.demo.wifi_throughput.viewmodel

import android.content.Context
import android.system.Os.socket
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.siliconlabs.bledemo.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.BufferedInputStream
import java.io.DataInputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.ConnectException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketTimeoutException
import java.security.KeyStore
import java.util.Timer
import java.util.TimerTask
import java.util.concurrent.TimeUnit
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLServerSocket
import javax.net.ssl.SSLServerSocketFactory


class WifiThroughputViewModel : ViewModel() {
    var bytesCountPerSec: Float = 0.0F
    private var timerTask: TimerTask? = null
    private var timer: Timer? = null
    var count = -1
    private val _updateSpeed = MutableLiveData<Float>()
    private var finalThroughPut = 0
    private var _perSecondLog =  MutableLiveData<MutableList<String>>()
    private var _updateFinalPackets =  MutableLiveData<MutableMap<String,String>>()
    private var _totalBytesInProgress = MutableLiveData<Long>()
    private var _waitingForConnection = MutableLiveData<String?>() // null when connected, role when waiting
    var finalBandwidth: Float = 0.0F
    //Tcp Server
    var totalBytesReceived = 0L
    var running: Boolean = false
    private var serverSocket: ServerSocket? = null
    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var _handleException =  MutableLiveData<Boolean>()
    private var _tlsHandleZeroBytes =  MutableLiveData<Boolean>()

    private var isExceptionOccured = false
    val mobileServerSocket = ServerSocket()
    var isConncetedToClient = MutableLiveData<Boolean>()
    var udpSocket = DatagramSocket()

    init {
        _perSecondLog.value = mutableListOf()
        _updateFinalPackets.value = mutableMapOf()
        _totalBytesInProgress.value = 0L
        _waitingForConnection.value = null
    }
    fun updateSpeed():LiveData<Float>{
        return _updateSpeed
    }

    fun updateLogsPerSecond():LiveData<MutableList<String>>{
        return _perSecondLog
    }

    fun updateFinalResult(): LiveData<MutableMap<String,String>>{
        return _updateFinalPackets
    }

    fun updateTotalBytesInProgress(): LiveData<Long> {
        return _totalBytesInProgress
    }

    fun waitingForConnection(): LiveData<String?> {
        return _waitingForConnection
    }

    fun handleException(): MutableLiveData<Boolean> {
        return _handleException
    }

    fun handleTlsZeroBytes(): MutableLiveData<Boolean>{
        return _tlsHandleZeroBytes
    }

    fun isConnceted(): MutableLiveData<Boolean> {
        return isConncetedToClient
    }

    fun tcpClient(ipAddress : String, portNumber : Int){
        println("TCP client started!!!")
        isExceptionOccured = false
        count = -1
        finalThroughPut = 0
        viewModelScope.launch(Dispatchers.Main) {
            _totalBytesInProgress.value = 0L
            _waitingForConnection.value = "server" // Waiting for server to accept connection
        }
        val TEST_TIMEOUT: Long = 30000 // Timeout in milliseconds
        val BYTES_TO_SEND = 536870912.0 //317470020// Example value, set your actual byte count
        val ip = ipAddress // Replace with your server's IP address
        val port = portNumber // Replace with your server's port
        val buffer = ByteArray(10240)
        var socket : Socket? = null
        var outputStream : OutputStream? = null
        var inputStream : InputStream? = null
        var totalBytesTransferred = 0L
        var startTime = 0L
        var start = 0L
        var endTime : Long = 0
        var isTimerStarted = false
        try{
            socket = Socket(ip, port)
            socket.soTimeout = 30000

            outputStream = socket.getOutputStream()
            inputStream = socket.getInputStream()

            while (totalBytesTransferred < BYTES_TO_SEND) {
                if(!isTimerStarted){
                    isTimerStarted = true
                    startTime = System.nanoTime()
                    start = System.currentTimeMillis()
                    startTimer()
                }

                outputStream.write(buffer)
                viewModelScope.launch(Dispatchers.Main) {
                    isConncetedToClient.value = true
                    _waitingForConnection.value = null // Connected, clear waiting message
                }
                totalBytesTransferred += buffer.size
                viewModelScope.launch(Dispatchers.Main) {
                    _totalBytesInProgress.value = totalBytesTransferred
                }
                addBytesToCount(buffer.size)

                val now = System.currentTimeMillis()
                if ((now - start) > TEST_TIMEOUT || count >= 30) {
                    println(
                        """
                        Time Out: ${now - start}
                        """.trimIndent()
                    )
                    break
                }
            }
            endTime = System.nanoTime()
            val timeTakenInSeconds = TimeUnit.NANOSECONDS.toSeconds(endTime - startTime)
            println("Time taken : " + timeTakenInSeconds)
            val throughputInMbps =
                (totalBytesTransferred * 8.0) / timeTakenInSeconds / 1_000_000
            println("TCP Throughput total bytes : $totalBytesTransferred")

            println("TCP Throughput: $throughputInMbps Mbps")

            inputStream?.close()
            outputStream?.close()
            cancelTimer()
        }catch (e: ConnectException){
            endTime = System.nanoTime()
            println("TCP Throughput: $e")
            isExceptionOccured = true
            cancelTimer()
        } catch (e: Exception){
            endTime = System.nanoTime()
            println("TCP Throughput: $e")
            //isExceptionOccured = true
            pollTimeoutExceptionHandling(e)
            cancelTimer()
        }finally {
            isTimerStarted = false
            if(!isExceptionOccured){
                val timeTakenInSeconds = TimeUnit.NANOSECONDS.toSeconds(endTime - startTime)
                val throughputInMbps =
                    (totalBytesTransferred * 8.0) / timeTakenInSeconds / 1_000_000
                //val throughputInMbps = (totalBytesTransferred/(1024*1024)) / timeTakenInSeconds / 1_000_000
                println("Time taken finally: " + timeTakenInSeconds)

                println("TCP Throughput total bytes finally : $totalBytesTransferred")

                println("TCP Throughput finally: $throughputInMbps Mbps")

                socket?.close()
                println("TCP Throughput finally : socket connection closed")
                cancelTimer()
                _updateFinalPackets.value?.put("transfer", totalBytesTransferred.toString())
                _updateFinalPackets.value?.put("bandwidth", String.format("%.2f", throughputInMbps).toString())
                viewModelScope.launch(Dispatchers.Main) {
                    _updateFinalPackets.value = _updateFinalPackets.value
                }
                _perSecondLog.value?.clear()
            }else{
                viewModelScope.launch(Dispatchers.Main) {
                    _handleException.value = isExceptionOccured
                }
            }
        }

    }

    private inner class PeriodicSpeedUpdate : TimerTask() {
        override fun run() {
            count++
            viewModelScope.launch(Dispatchers.Main) {
                val throughputInMBps : Float = bytesCountPerSec//bytesToKB(bytesCountPerSec)
                val bandwidth = ((bytesCountPerSec*8.388608)/(1000*1000)).toFloat()
                finalBandwidth += bandwidth

                var bandwidthPerSecondInString = bytesToHumanReadableSize(throughputInMBps)
                var bandwidthPerSecond = ""

                // removing duplicate whitespace
                println("Extra whitespaces removed: "
                        + bandwidthPerSecondInString.replace("\\s+".toRegex(), " "))
                // removing all the whitespaces
                println("After removing all the whitespaces: "
                        + bandwidthPerSecondInString.replace("\\s+".toRegex(), ""))
                if (bandwidthPerSecondInString.contains("kB", ignoreCase = true)){
                    println("After removing KB: "
                            + bandwidthPerSecondInString.replace("kB+".toRegex(), ""))

                    bandwidthPerSecond = String.format("%.2f", ((bandwidthPerSecondInString.replace("kB+".toRegex(), "").toFloat()*8388.608)/1000000))
                }else if(bandwidthPerSecondInString.contains("MB", ignoreCase = true)){
                    println("After removing MB: "
                            + bandwidthPerSecondInString.replace("MB+".toRegex(), ""))

                    bandwidthPerSecond = String.format("%.2f", (bandwidthPerSecondInString.replace("MB+".toRegex(), "").toFloat()*8.388608))
                }else{
                    println("After removing MB: "
                            + bandwidthPerSecondInString.replace("bytes+".toRegex(), ""))

                    bandwidthPerSecond = String.format("%.2f", (bandwidthPerSecondInString.replace("bytes+".toRegex(), "").toFloat()*8.388608))
                }
                if (count >= 30 && totalBytesReceived == 0L){
                    cancelTimer()
                    _tlsHandleZeroBytes.value = true
                }
                if (count > 0){
                    _updateSpeed.value = bandwidthPerSecond.toFloat()
                    _perSecondLog.value?.add(getSecondInfo(count, throughputInMBps, bandwidthPerSecond))
                    _perSecondLog.value = _perSecondLog.value
                }
                bytesCountPerSec = 0F
            }
        }
    }

    fun tcpServer(portNumber : Int) {
        running = true
        count = -1
        viewModelScope.launch(Dispatchers.Main) {
            _perSecondLog.value = mutableListOf()
            _perSecondLog.value =  _perSecondLog.value
            isConncetedToClient.value = false
            _totalBytesInProgress.value = 0L
            _waitingForConnection.value = "client" // Waiting for client to connect
        }
        isExceptionOccured = false
        mobileServerSocket.soTimeout = 30000
        try {
            if (mobileServerSocket.isBound){
                println("isBound true")
                return
            }else{
                mobileServerSocket.bind(InetSocketAddress(portNumber))
            }
            //if (!serverSocket.isBound)serverSocket.bind(InetSocketAddress(portNumber))
            println("Server listening on port $portNumber")
            while (running) {
                val clientSocket = mobileServerSocket.accept()
                handleClient(clientSocket!!)
            }
        } catch (e: Exception) {
            pollTimeoutExceptionHandling(e)
            println("Exception ${e.message}")
        } finally {
            //mobileServerSocket.close()
            running = false
            try {
                if (mobileServerSocket != null && !mobileServerSocket.isClosed()) {
                    mobileServerSocket.close()
                }
            } catch (e: IOException) {
                // Handle socket closing exceptions
                if (isExceptionOccured){
                    viewModelScope.launch(Dispatchers.Main) {
                        _handleException.value = isExceptionOccured
                    }
                }
            }
        }
    }

    fun incrementBytesReceived(bytes: Int) {
        totalBytesReceived += bytes
        viewModelScope.launch(Dispatchers.Main) {
            _totalBytesInProgress.value = totalBytesReceived
        }
    }

    private  fun handleClient(clientSocket: Socket)  {
        println("Client connected: ${clientSocket.inetAddress.hostAddress}")
        viewModelScope.launch(Dispatchers.Main) {
            isConncetedToClient.value = true
            _waitingForConnection.value = null // Connected, clear waiting message
        }
        startTimer()
        val dataInputStream = DataInputStream(BufferedInputStream(clientSocket.getInputStream()))
        val startTime = System.nanoTime()
        try {
            while (true) {
                val bytesRead = dataInputStream.read(ByteArray(2460))
                if (bytesRead == -1) break
                incrementBytesReceived(bytesRead)
                addBytesToCount(bytesRead)
            }
        } catch (e: IOException) {println("Error handling client: ${e.message}")
        } finally {
            cancelTimer()
            dataInputStream.close()
            clientSocket.close()
            println("Client disconnected")
            // Calculate and report throughput every second
            coroutineScope.launch {
                val endTime = System.nanoTime()
                val timeTakenInSeconds = TimeUnit.NANOSECONDS.toSeconds(endTime - startTime)
                val throughputInMbps = (totalBytesReceived * 8.388608) / timeTakenInSeconds / 1_000_000
                println("Server Throughput total time taken : $timeTakenInSeconds")
                // Update UI or log the throughput
                println("Server Throughput total count : $totalBytesReceived")
                println("Server Throughput: $throughputInMbps Mbps")
                _updateFinalPackets.value?.put("transfer", totalBytesReceived.toString())
                _updateFinalPackets.value?.put("bandwidth", String.format("%.2f", throughputInMbps).toString())
                viewModelScope.launch(Dispatchers.Main) {
                    _updateFinalPackets.value = _updateFinalPackets.value
                }
                _perSecondLog.value?.clear()
                totalBytesReceived = 0
                count = -1
            }
        }
    }

    fun stop() {
        coroutineScope.cancel()
        serverSocket?.close()
        mobileServerSocket.close()
        sslServerSocket?.close()
        udpSocket?.close()
    }

    companion object {
        const val DISPLAY_REFRESH_PERIOD: Long = 1000 // in milliseconds
    }

    private fun startTimer() {
        if (timer == null) {
            timer = Timer()
            timerTask = PeriodicSpeedUpdate()
            timer?.scheduleAtFixedRate(timerTask,
                WifiThroughputViewModel.DISPLAY_REFRESH_PERIOD,
                WifiThroughputViewModel.DISPLAY_REFRESH_PERIOD
            )
        }
    }

    private fun cancelTimer() {
        bytesCountPerSec = 0F
        timerTask?.cancel()
        timer?.cancel()
        timer?.purge()
        timerTask = null
        timer = null
    }

    fun addBytesToCount(packetSize: Int) {
        bytesCountPerSec += packetSize
    }

    fun getSecondInfo(count : Int, throughPut: Float, bandwidth: String): String{
        return "$count-${count+1} Sec,${bytesToHumanReadableSize(throughPut)},$bandwidth"
    }

    fun bytesToHumanReadableSize(bytes: Float) = when {
        bytes >= 1 shl 30 -> "%.1f GB".format(bytes / (1 shl 30))
        bytes >= 1 shl 20 -> "%.1f MB".format(bytes / (1 shl 20))
        bytes >= 1 shl 10 -> "%.0f kB".format(bytes / (1 shl 10))
        else -> "$bytes bytes"
    }

    fun udpClient(ipAddress : String, portNumber : Int){
        isExceptionOccured = false
        count = -1
        finalThroughPut = 0
        viewModelScope.launch(Dispatchers.Main) {
            _totalBytesInProgress.value = 0L
            _waitingForConnection.value = "server" // Waiting for server
        }
        val BYTES_TO_SEND = 536870912.0 //317470020// Example value, set your actual byte count
        val TEST_TIMEOUT: Long = 30000 // Timeout in milliseconds
        val serverAddress: InetAddress
        var socket: DatagramSocket? = null
        var totalBytesSent = 0
        var sentBytes: Int
        var fail = 0
        var pass = 0
        val SEVER_IP = ipAddress
        var endTime : Long = 0
        var totalBytesTransferred = 0L
        val buffer = ByteArray(1470)
        var isTimerStarted = false
        var startTime = System.nanoTime()

        try {
            serverAddress = InetAddress.getByName(SEVER_IP) // Replace with actual server address
            socket = DatagramSocket()
            socket.soTimeout = TEST_TIMEOUT.toInt()
            var start = System.currentTimeMillis()
            if(!isTimerStarted){
                isTimerStarted = true
                startTime = System.nanoTime()
                start = System.currentTimeMillis()
                startTimer()
            }
            val packet = DatagramPacket(
                buffer,
                buffer.size,
                serverAddress,
                portNumber
            )
            while (totalBytesTransferred < BYTES_TO_SEND) {
                println("Total bytes sent so far : $totalBytesTransferred")
                socket.send(packet)
                if (totalBytesTransferred == 0L) {
                    viewModelScope.launch(Dispatchers.Main) {
                        _waitingForConnection.value = null // Started sending, clear waiting message
                    }
                }
                val now = System.currentTimeMillis()

                if ((now - start) > TEST_TIMEOUT || count >= 30) {
                    println(
                        """
                        Time Out: ${now - start}
                        """.trimIndent()
                    )
                    //socket.close()
                    break
                }
                sentBytes = packet.length
                if (sentBytes < 0) {
                    println("FAIL - : $fail")

                    fail++
                } else {
                    pass++
                }
                totalBytesTransferred += sentBytes

                viewModelScope.launch(Dispatchers.Main) {
                    _totalBytesInProgress.value = totalBytesTransferred
                }
                addBytesToCount(sentBytes)
                if (sentBytes > 0) {
                    totalBytesSent += sentBytes
                }
                println("Total buffer.size : ${buffer.size}")

                totalBytesTransferred += buffer.size
                addBytesToCount(buffer.size)
            }
            endTime = System.nanoTime()
            val timeTakenInSeconds = TimeUnit.NANOSECONDS.toSeconds(endTime - startTime)
            val throughputInMbps =
                (totalBytesTransferred * 8.0) / timeTakenInSeconds / 1_000_000
            cancelTimer()
        } catch (e: Exception) {
            e.printStackTrace()
            cancelTimer()
            socket?.close()
        } finally {
            if (socket != null && !socket.isClosed) {
                socket.close()
            }

            if(!isExceptionOccured){
                val timeTakenInSeconds = TimeUnit.NANOSECONDS.toSeconds(endTime - startTime)
                val throughputInMbps =
                    (totalBytesTransferred * 8.0) / timeTakenInSeconds / 1_000_000
                socket?.close()
                cancelTimer()
                _updateFinalPackets.value?.put("transfer", totalBytesTransferred.toString())
                _updateFinalPackets.value?.put("bandwidth", String.format("%.2f", throughputInMbps).toString())
                viewModelScope.launch(Dispatchers.Main) {
                    _updateFinalPackets.value = _updateFinalPackets.value
                }
               // _perSecondLog.value?.clear()
               println("throughPut speed data are getting cleared to zero!!!")
            }else{
                viewModelScope.launch(Dispatchers.Main) {
                    _handleException.value = isExceptionOccured
                }
            }
        }
    }

    fun udpServer(portNumber : Int){
        val port = portNumber
        var totalBytesReceived =0L

        running = true
        count = -1
        viewModelScope.launch(Dispatchers.Main) {
            _perSecondLog.value = mutableListOf()
            _perSecondLog.value =  _perSecondLog.value
            _totalBytesInProgress.value = 0L
            _waitingForConnection.value = "client" // Waiting for client
        }
        // Coroutine to receive UDP packets
        coroutineScope.launch {
            DatagramSocket(port).use { socket ->
                println("UDP Server listening on port $port")
                udpSocket = socket
                udpSocket.soTimeout = 30000
                val buffer = ByteArray(1470)
                var startTime = 0L
                var firstTime = true
                try {
                    while (true) {
                        val packet = DatagramPacket(buffer, buffer.size)
                        udpSocket.receive(packet)

                        if (firstTime){
                            startTime = System.nanoTime()
                            firstTime = false
                        }
                        if (packet.length > 0){
                            viewModelScope.launch(Dispatchers.Main) {
                                isConncetedToClient.value = true
                                _waitingForConnection.value = null // Received data, clear waiting message
                            }
                            udpSocket.soTimeout = 500
                            startTimer()
                        }
                        println("Server Throughput bytes: ${packet.length}")
                        totalBytesReceived += packet.length
                        println("totalBytesReceived  : $totalBytesReceived")
                        viewModelScope.launch(Dispatchers.Main) {
                            _totalBytesInProgress.value = totalBytesReceived
                        }
                        incrementBytesReceived(packet.length)
                        addBytesToCount(packet.length)
                    }
                } catch (e: Exception) {
                    cancelTimer()
                    if (count <= 0){
                        pollTimeoutExceptionHandling(e)
                    }
                    println("Error handling client: ${e.message}")
                } finally {
                    if (!isExceptionOccured){
                        firstTime = true
                        cancelTimer()
                        println("Client disconnected")
                        // Calculate and report throughput every second
                        coroutineScope.launch {
                            val endTime = System.nanoTime()
                            val timeTakenInSeconds = TimeUnit.NANOSECONDS.toSeconds(endTime - startTime)
                            val throughputInMbps = (totalBytesReceived * 8.0) / timeTakenInSeconds / 1_000_000
                            println("Server Throughput total time taken : $timeTakenInSeconds")
                            // Update UI or log the throughput
                            _updateFinalPackets.value?.put("transfer", totalBytesReceived.toString())
                            _updateFinalPackets.value?.put("bandwidth", String.format("%.2f", throughputInMbps).toString())
                            viewModelScope.launch(Dispatchers.Main) {
                                _updateFinalPackets.value = _updateFinalPackets.value
                            }
                            _perSecondLog.value?.clear()
                            totalBytesReceived = 0
                            count = -1
                        }
                    }else{
                        viewModelScope.launch(Dispatchers.Main) {
                            _handleException.value = isExceptionOccured
                        }
                    }
                }
            }
        }
    }

    fun createSSLContext(context: Context): SSLContext {
        val keyStore = KeyStore.getInstance("PKCS12")
        val password = "Whyyouneed!#2024".toCharArray() // Use the same password you used to create the .p12 file

        val inputStream: InputStream = context.resources.openRawResource(R.raw.keystore) // Put server.p12 in res/raw
        keyStore.load(inputStream, password)

        val keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
        keyManagerFactory.init(keyStore, password)

        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(keyManagerFactory.keyManagers, null, null)

        return sslContext
    }

    private var sslServerSocket: SSLServerSocket? = null

    fun startTLSServer(port: Int, context: Context, isUpload: Boolean) {
        count = -1
        isExceptionOccured = false
        println("Port number : $port")
        viewModelScope.launch(Dispatchers.Main) {
            _perSecondLog.value = mutableListOf()
            _perSecondLog.value =  _perSecondLog.value
            isConncetedToClient.value = false
            _totalBytesInProgress.value = 0L
            _waitingForConnection.value = "client" // Waiting for client
        }
        try {
            val sslContext = createSSLContext(context)
            val factory: SSLServerSocketFactory = sslContext.serverSocketFactory
            sslServerSocket = factory.createServerSocket(port) as SSLServerSocket
            sslServerSocket?.soTimeout = 30000
            sslServerSocket?.let {
                it.useTLSProtocols()
                while (true) {
                    val socket = it.accept() // Wait for client connections
                    println("Accept done")
                    viewModelScope.launch(Dispatchers.Main) {
                        isConncetedToClient.value = true
                        _waitingForConnection.value = null // Client connected, clear waiting message
                    }
                    // handleTLSClient(socket) // Handle client in separate thread
                    if (!isUpload){
                        handleClient(socket)
                    }else{
                        uploadData(socket!!)
                    }
                }
            }
        } catch (e: Exception) {
            pollTimeoutExceptionHandling(e)
            e.printStackTrace()
        }finally {
            try {
                if (sslServerSocket != null && !sslServerSocket!!.isClosed()) {
                    sslServerSocket!!.close()
                }
            } catch (e: IOException) {
                // Handle socket closing exceptions
                if (isExceptionOccured){
                    viewModelScope.launch(Dispatchers.Main) {
                        _handleException.value = isExceptionOccured
                    }
                }
            }
        }
    }

    private fun SSLServerSocket.useTLSProtocols() {
        // Set your required TLS protocols
        enabledProtocols = arrayOf("TLSv1.2", "TLSv1.3") // Specify protocols as needed
    }

    fun uploadData(socket: Socket){
        isExceptionOccured = false
        finalThroughPut = 0
        viewModelScope.launch(Dispatchers.Main) {
            _totalBytesInProgress.value = 0L
            _waitingForConnection.value = "server" // TLS client waiting for server (already accepted but starting transfer)
        }
        val TEST_TIMEOUT: Long = 30000 // Timeout in milliseconds
        val BYTES_TO_SEND = 536870912.0 //317470020// Example value, set your actual byte count
        val buffer = ByteArray(1370)

        var outputStream : OutputStream? = null
        var totalBytesTransferred = 0L
        var startTime = 0L
        var start = 0L
        var endTime : Long = 0
        var isTimerStarted = false
        try{
            outputStream = socket.getOutputStream()
            outputStream!!.write(buffer)
            viewModelScope.launch {
                delay(800L)
            }
            while (totalBytesTransferred < BYTES_TO_SEND) {
                if(!isTimerStarted){
                    isTimerStarted = true
                    startTime = System.nanoTime()
                    start = System.currentTimeMillis()
                    startTimer()
                }
                outputStream = socket.getOutputStream()
                outputStream!!.write(buffer)
                totalBytesTransferred += buffer.size
                viewModelScope.launch(Dispatchers.Main) {
                    _totalBytesInProgress.value = totalBytesTransferred
                    if (totalBytesTransferred == buffer.size.toLong()) {
                        _waitingForConnection.value = null // Started transferring, clear waiting message
                    }
                }
                addBytesToCount(buffer.size)

                val now = System.currentTimeMillis()
                if ((now - start) > TEST_TIMEOUT || count >= 30) {
                    println(
                        """
                        Time Out: ${now - start}
                        """.trimIndent()
                    )
                    break
                }
            }
            endTime = System.nanoTime()
            val timeTakenInSeconds = TimeUnit.NANOSECONDS.toSeconds(endTime - startTime)
            println("Time taken : " + timeTakenInSeconds)
            val throughputInMbps =
                (totalBytesTransferred * 8.0) / timeTakenInSeconds / 1_000_000
            println("TCP Throughput total bytes : $totalBytesTransferred")

            println("TCP Throughput: $throughputInMbps Mbps")

            outputStream?.close()
            cancelTimer()
        }catch (e: Exception){
            endTime = System.nanoTime()
            println("TCP Throughput: $e")
            //isExceptionOccured = true
            cancelTimer()
        }finally {
            isTimerStarted = false
            count = -1
            if(!isExceptionOccured){
                val timeTakenInSeconds = TimeUnit.NANOSECONDS.toSeconds(endTime - startTime)
                val throughputInMbps =
                    (totalBytesTransferred * 8.0) / timeTakenInSeconds / 1_000_000
                socket.close()
                println("TCP Throughput finally : socket connection closed")
                cancelTimer()
                _updateFinalPackets.value?.put("transfer", totalBytesTransferred.toString())
                _updateFinalPackets.value?.put("bandwidth", String.format("%.2f", throughputInMbps).toString())
                viewModelScope.launch(Dispatchers.Main) {
                    _updateFinalPackets.value = _updateFinalPackets.value
                }
                _perSecondLog.value?.clear()
            }else{
                viewModelScope.launch(Dispatchers.Main) {
                    _handleException.value = isExceptionOccured
                }
            }
        }
    }

    private fun pollTimeoutExceptionHandling(e: Exception) {
        if (e is SocketTimeoutException){
            isExceptionOccured = true
        }
    }
}
