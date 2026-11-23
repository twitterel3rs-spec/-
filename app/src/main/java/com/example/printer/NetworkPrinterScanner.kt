package com.example.printer

import android.content.Context
import android.net.wifi.WifiManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.Socket
import java.util.Collections

data class DiscoveredPrinter(
    val ip: String,
    val port: Int = 9100,
    val hostName: String = "",
    val displayName: String = "طابعة شبكة حرارية ($ip:$port)"
)

/**
 * High-speed local network scanner for ESC/POS thermal printers (port 9100 & 515).
 * Discovers network printers across the local Wi-Fi subnet in ~2 seconds.
 */
object NetworkPrinterScanner {

    suspend fun scanLocalNetworkForPrinters(context: Context): List<DiscoveredPrinter> = withContext(Dispatchers.IO) {
        val subnetPrefixes = getLocalSubnetPrefixes(context)
        if (subnetPrefixes.isEmpty()) {
            return@withContext emptyList()
        }

        val foundPrinters = Collections.synchronizedList(mutableListOf<DiscoveredPrinter>())
        val semaphore = Semaphore(20)

        coroutineScope {
            val scanJobs = mutableListOf<kotlinx.coroutines.Deferred<*>>()

            for (subnet in subnetPrefixes) {
                // Scan typical host range 1..254
                for (host in 1..254) {
                    val targetIp = "$subnet$host"
                    val job = async(Dispatchers.IO) {
                        semaphore.withPermit {
                            try {
                                if (checkPrinterPort(targetIp, 9100)) {
                                    foundPrinters.add(DiscoveredPrinter(targetIp, 9100, "ESC/POS Thermal (Port 9100)"))
                                } else if (checkPrinterPort(targetIp, 515)) {
                                    foundPrinters.add(DiscoveredPrinter(targetIp, 515, "LPD Thermal (Port 515)"))
                                }
                            } catch (_: Throwable) {}
                        }
                    }
                    scanJobs.add(job)
                }
            }

            scanJobs.awaitAll()
        }

        foundPrinters.toList().sortedBy { it.ip }
    }

    private fun checkPrinterPort(ip: String, port: Int, timeoutMs: Int = 300): Boolean {
        var socket: Socket? = null
        return try {
            socket = Socket()
            socket.connect(InetSocketAddress(ip, port), timeoutMs)
            true
        } catch (_: Exception) {
            false
        } finally {
            try {
                socket?.close()
            } catch (_: Exception) {}
        }
    }

    private fun getLocalSubnetPrefixes(context: Context): Set<String> {
        val subnets = mutableSetOf<String>()

        try {
            // Method 1: Check Network Interfaces
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val iface = interfaces.nextElement()
                if (iface.isLoopback || !iface.isUp) continue

                val addrs = iface.inetAddresses
                while (addrs.hasMoreElements()) {
                    val addr = addrs.nextElement()
                    val hostAddress = addr.hostAddress
                    if (hostAddress != null && !addr.isLoopbackAddress && hostAddress.contains(".")) {
                        val parts = hostAddress.split(".")
                        if (parts.size == 4) {
                            subnets.add("${parts[0]}.${parts[1]}.${parts[2]}.")
                        }
                    }
                }
            }
        } catch (_: Exception) {}

        // Method 2: WifiManager fallback
        try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            val ipInt = wifiManager?.connectionInfo?.ipAddress ?: 0
            if (ipInt != 0) {
                val ipStr = String.format(
                    java.util.Locale.US,
                    "%d.%d.%d.",
                    ipInt and 0xff,
                    ipInt shr 8 and 0xff,
                    ipInt shr 16 and 0xff
                )
                subnets.add(ipStr)
            }
        } catch (_: Exception) {}

        // Fallback default subnets if unable to determine
        if (subnets.isEmpty()) {
            subnets.add("192.168.1.")
            subnets.add("192.168.0.")
        }

        return subnets
    }
}
