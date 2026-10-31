package com.myra.assistant.connect

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log

/**
 * Handles mDNS / Network Service Discovery (NSD) for MYRA Connect.
 * Discovers companion phones automatically across the local Wi-Fi / Hotspot subnet
 * without hardcoded IP addresses, with manual IP/Port fallback.
 */
class MyraNsdManager(
    private val context: Context,
    private val thisDeviceName: String
) {
    private val tag = "MYRA_NSD"
    private val nsdManager = context.getSystemService(Context.NSD_SERVICE) as? NsdManager

    companion object {
        const val SERVICE_TYPE = "_myraconnect._tcp."
    }

    private var registrationListener: NsdManager.RegistrationListener? = null
    private var discoveryListener: NsdManager.DiscoveryListener? = null
    private var isRegistered = false
    private var isDiscovering = false

    /**
     * Broadcasts this phone as an available MYRA Connect endpoint on the local hotspot.
     */
    fun registerService(port: Int) {
        if (nsdManager == null || isRegistered) return

        val serviceInfo = NsdServiceInfo().apply {
            serviceName = "MYRA-${thisDeviceName.replace(" ", "-")}"
            serviceType = SERVICE_TYPE
            setPort(port)
        }

        registrationListener = object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(serviceInfo: NsdServiceInfo?) {
                isRegistered = true
                Log.d(tag, "NSD service registered: ${serviceInfo?.serviceName} on port $port")
            }

            override fun onRegistrationFailed(serviceInfo: NsdServiceInfo?, errorCode: Int) {
                Log.w(tag, "NSD registration failed: code $errorCode")
                isRegistered = false
            }

            override fun onServiceUnregistered(serviceInfo: NsdServiceInfo?) {
                isRegistered = false
                Log.d(tag, "NSD service unregistered")
            }

            override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo?, errorCode: Int) {
                Log.w(tag, "NSD unregistration failed: code $errorCode")
            }
        }

        try {
            nsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener)
        } catch (e: Exception) {
            Log.e(tag, "Error registering NSD service", e)
        }
    }

    fun unregisterService() {
        if (nsdManager != null && isRegistered && registrationListener != null) {
            try {
                nsdManager.unregisterService(registrationListener)
            } catch (_: Exception) {}
            isRegistered = false
            registrationListener = null
        }
    }

    /**
     * Scans for other MYRA phones broadcasting on the local hotspot.
     */
    fun startDiscovery(onDiscovered: (name: String, host: String, port: Int) -> Unit) {
        if (nsdManager == null || isDiscovering) return

        discoveryListener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(regType: String?) {
                isDiscovering = true
                Log.d(tag, "NSD discovery started for $regType")
            }

            override fun onServiceFound(serviceInfo: NsdServiceInfo?) {
                if (serviceInfo == null) return
                if (serviceInfo.serviceType == SERVICE_TYPE || serviceInfo.serviceType.startsWith("_myraconnect")) {
                    Log.d(tag, "Found service: ${serviceInfo.serviceName}")
                    resolveService(serviceInfo, onDiscovered)
                }
            }

            override fun onServiceLost(serviceInfo: NsdServiceInfo?) {
                Log.d(tag, "Service lost: ${serviceInfo?.serviceName}")
            }

            override fun onDiscoveryStopped(serviceType: String?) {
                isDiscovering = false
                Log.d(tag, "NSD discovery stopped")
            }

            override fun onStartDiscoveryFailed(serviceType: String?, errorCode: Int) {
                isDiscovering = false
                Log.w(tag, "NSD start discovery failed: code $errorCode")
            }

            override fun onStopDiscoveryFailed(serviceType: String?, errorCode: Int) {
                Log.w(tag, "NSD stop discovery failed: code $errorCode")
            }
        }

        try {
            nsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
        } catch (e: Exception) {
            Log.e(tag, "Failed to start NSD discovery", e)
        }
    }

    private fun resolveService(
        serviceInfo: NsdServiceInfo,
        onResolved: (name: String, host: String, port: Int) -> Unit
    ) {
        val resolveListener = object : NsdManager.ResolveListener {
            override fun onResolveFailed(serviceInfo: NsdServiceInfo?, errorCode: Int) {
                Log.w(tag, "Resolve failed for ${serviceInfo?.serviceName}: code $errorCode")
            }

            override fun onServiceResolved(resolvedInfo: NsdServiceInfo?) {
                if (resolvedInfo == null) return
                val host = resolvedInfo.host?.hostAddress ?: return
                val port = resolvedInfo.port
                val name = resolvedInfo.serviceName.removePrefix("MYRA-").replace("-", " ")
                Log.d(tag, "Resolved MYRA device: $name at $host:$port")
                onResolved(name, host, port)
            }
        }

        try {
            nsdManager?.resolveService(serviceInfo, resolveListener)
        } catch (e: Exception) {
            Log.e(tag, "Error resolving service", e)
        }
    }

    fun stopDiscovery() {
        if (nsdManager != null && isDiscovering && discoveryListener != null) {
            try {
                nsdManager.stopServiceDiscovery(discoveryListener)
            } catch (_: Exception) {}
            isDiscovering = false
            discoveryListener = null
        }
    }
}
