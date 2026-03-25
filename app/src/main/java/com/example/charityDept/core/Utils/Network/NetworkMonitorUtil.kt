//package com.example.charityDept.core.Utils.Network

package com.example.charityDept.core.Utils.Network

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import androidx.annotation.RequiresPermission
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

@Singleton
class NetworkMonitorUtil @Inject constructor(
    @ApplicationContext context: Context
) {
    private val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val _isOnline = MutableStateFlow(checkNow(cm, validated = true))
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network)          { _isOnline.value = checkNow(cm, validated = false) }
        override fun onLost(network: Network)               { _isOnline.value = checkNow(cm, validated = false) }
        override fun onCapabilitiesChanged(n: Network, c: NetworkCapabilities) {
            _isOnline.value = checkNow(cm, validated = true)
        }
    }

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            cm.registerDefaultNetworkCallback(callback)
        } else {
            cm.registerNetworkCallback(NetworkRequest.Builder().build(), callback)
        }
        _isOnline.value = checkNow(cm, validated = false)
    }

    /** Synchronous snapshot */
    fun onlineNow(): Boolean = _isOnline.value

    /**
     * Suspend until online (or return immediately if already online).
     * Useful to "wait briefly, else fall back to cache".
     */
    suspend fun waitForOnline(): Unit = suspendCancellableCoroutine { cont ->
        if (onlineNow()) { cont.resume(Unit); return@suspendCancellableCoroutine }
        // One-shot temp callback
        val oneShot = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                try { cm.unregisterNetworkCallback(this) } catch (_: Exception) {}
                if (cont.isActive) cont.resume(Unit)
            }
        }
        cm.registerNetworkCallback(NetworkRequest.Builder().build(), oneShot)
        cont.invokeOnCancellation {
            try { cm.unregisterNetworkCallback(oneShot) } catch (_: Exception) {}
        }
    }

    /** If you ever need to clean up manually (usually not needed for a @Singleton) */
    fun stop() {
        try { cm.unregisterNetworkCallback(callback) } catch (_: Exception) {}
    }

    @SuppressLint("MissingPermission")
    @RequiresPermission(android.Manifest.permission.ACCESS_NETWORK_STATE)
    private fun checkNow(cm: ConnectivityManager, validated: Boolean): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val nw = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(nw) ?: return false
            val hasTransport =
                caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                        caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                        caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)

            val hasInternet = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            val isValidated = !validated || caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            hasTransport && hasInternet && isValidated
        } else {
            @Suppress("DEPRECATION")
            cm.activeNetworkInfo?.isConnected == true
        }
    }
}

//import android.annotation.SuppressLint
//import android.content.Context
//import android.net.ConnectivityManager
//import android.net.Network
//import android.net.NetworkCapabilities
//import android.os.Build
//import dagger.hilt.android.qualifiers.ApplicationContext
//import kotlinx.coroutines.flow.MutableStateFlow
//import kotlinx.coroutines.flow.StateFlow
//import javax.inject.Inject
//import javax.inject.Singleton
//
//// …other imports…
//
//@Singleton
//class NetworkMonitorUtil @Inject constructor(
//    @ApplicationContext context: Context
//) {
//    private val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
//
//    private val _isOnline = MutableStateFlow(checkNow(cm))
//    val isOnline: StateFlow<Boolean> = _isOnline
//
//    private val callback = object : ConnectivityManager.NetworkCallback() {
//        override fun onAvailable(network: Network) { _isOnline.value = checkNow(cm) }
//        override fun onLost(network: Network) { _isOnline.value = checkNow(cm) }
//        override fun onCapabilitiesChanged(network: Network, nc: NetworkCapabilities) { _isOnline.value = checkNow(cm) }
//    }
//
//    init {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
//            cm.registerDefaultNetworkCallback(callback)
//        } else {
//            cm.registerNetworkCallback(android.net.NetworkRequest.Builder().build(), callback)
//        }
//        _isOnline.value = checkNow(cm)
//    }
//
//    @SuppressLint("MissingPermission")
//    private fun checkNow(cm: ConnectivityManager): Boolean {
//        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//            val nw = cm.activeNetwork ?: return false
//            val caps = cm.getNetworkCapabilities(nw) ?: return false
//            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
//                    (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
//                            || caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
//                            || caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET))
//        } else {
//            @Suppress("DEPRECATION")
//            val info = cm.activeNetworkInfo
//            @Suppress("DEPRECATION")
//            info != null && info.isConnected
//        }
//    }
//}

