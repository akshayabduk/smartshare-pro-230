package com.smartshare.app.domain.share.transport

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.NetworkInfo
import android.net.Uri
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pInfo
import android.net.wifi.p2p.WifiP2pManager
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket

/**
 * PUBLIC_INTERFACE
 * WifiDirectShareTransport uses Wi‑Fi P2P (Wi‑Fi Direct) to discover peers, connect,
 * and open a client socket to the group owner to push a file. The remote peer must run
 * a corresponding server socket.
 */
class WifiDirectShareTransport(private val context: Context) : ShareTransport {

    private val manager: WifiP2pManager? =
        context.getSystemService(Context.WIFI_P2P_SERVICE) as? WifiP2pManager
    private val channel: WifiP2pManager.Channel? = manager?.initialize(context, context.mainLooper, null)

    override fun startDiscovery(activity: Activity, onReady: (PeerSession) -> Unit, onError: (String) -> Unit) {
        val mgr = manager
        val ch = channel
        if (mgr == null || ch == null) {
            onError("Wi‑Fi Direct not supported on this device")
            return
        }

        // Check required permissions before discovery or peer requests
        if (!hasWifiP2pPermission(activity)) {
            onError("Wi‑Fi Direct permission not granted")
            return
        }

        val devices = mutableListOf<WifiP2pDevice>()
        val deviceNamesAdapter = ArrayAdapter<String>(activity, android.R.layout.simple_list_item_1)
        val listView = ListView(activity).apply { adapter = deviceNamesAdapter }

        val dialog = android.app.AlertDialog.Builder(activity)
            .setTitle("Select a peer")
            .setView(listView)
            .setNegativeButton("Cancel") { d, _ -> d.dismiss() }
            .create()

        val filter = IntentFilter().apply {
            addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
        }

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                when (intent?.action) {
                    WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION -> {
                        if (!hasWifiP2pPermission(activity)) return
                        try {
                            mgr.requestPeers(ch) { list ->
                                devices.clear()
                                devices.addAll(list.deviceList)
                                deviceNamesAdapter.clear()
                                if (devices.isEmpty()) {
                                    deviceNamesAdapter.add("No devices found yet")
                                } else {
                                    devices.forEach { d -> deviceNamesAdapter.add("${d.deviceName} (${d.deviceAddress})") }
                                }
                                deviceNamesAdapter.notifyDataSetChanged()
                            }
                        } catch (_: SecurityException) {
                            onError("Wi‑Fi Direct permission missing")
                        }
                    }
                    WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION -> {
                        val netInfo = intent.getParcelableExtra<NetworkInfo>(WifiP2pManager.EXTRA_NETWORK_INFO)
                        if (netInfo?.isConnected == true) {
                            if (!hasWifiP2pPermission(activity)) return
                            try {
                                mgr.requestConnectionInfo(ch) { info: WifiP2pInfo ->
                                    val hostAddr = info.groupOwnerAddress ?: return@requestConnectionInfo
                                    val host = hostAddr.hostAddress ?: return@requestConnectionInfo
                                    val session = WifiDirectPeerSession(host)
                                    dialog.dismiss()
                                    onReady(session)
                                }
                            } catch (_: SecurityException) {
                                onError("Wi‑Fi Direct permission missing")
                            }
                        }
                    }
                }
            }
        }

        listView.setOnItemClickListener { _, _, position, _ ->
            if (devices.isEmpty()) return@setOnItemClickListener
            if (position < 0 || position >= devices.size) return@setOnItemClickListener
            val device = devices[position]
            val config = WifiP2pConfig().apply { deviceAddress = device.deviceAddress }

            if (!hasWifiP2pPermission(activity)) {
                onError("Wi‑Fi Direct permission not granted")
                return@setOnItemClickListener
            }

            try {
                mgr.connect(ch, config, object : WifiP2pManager.ActionListener {
                    override fun onSuccess() {
                        Toast.makeText(activity, "Connecting to ${device.deviceName}", Toast.LENGTH_SHORT).show()
                    }
                    override fun onFailure(reason: Int) {
                        onError("Connect failed: $reason")
                    }
                })
            } catch (_: SecurityException) {
                onError("Wi‑Fi Direct permission missing")
            }
        }

        // Initialize list with a placeholder
        deviceNamesAdapter.clear()
        deviceNamesAdapter.add("Discovering…")
        deviceNamesAdapter.notifyDataSetChanged()

        activity.registerReceiver(receiver, filter)
        dialog.setOnDismissListener {
            try { activity.unregisterReceiver(receiver) } catch (_: Throwable) {}
        }

        try {
            mgr.discoverPeers(ch, object : WifiP2pManager.ActionListener {
                override fun onSuccess() {
                    dialog.show()
                }
                override fun onFailure(reason: Int) {
                    onError("Discovery failed: $reason")
                }
            })
        } catch (_: SecurityException) {
            onError("Wi‑Fi Direct permission missing")
        }
    }

    private class WifiDirectPeerSession(private val host: String) : PeerSession {
        private var socket: Socket? = null
        private val port = 8988 // sample port; remote should listen here

        override suspend fun sendFile(context: Context, uri: Uri, displayName: String, onProgress: (Int) -> Unit) {
            withContext(Dispatchers.IO) {
                val s = Socket()
                socket = s
                try {
                    s.connect(InetSocketAddress(host, port), 8000)
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        val out: OutputStream = s.getOutputStream()
                        val nameBytes = displayName.toByteArray(Charsets.UTF_8)
                        val header = ByteArray(4)
                        val len = nameBytes.size
                        header[0] = ((len shr 24) and 0xFF).toByte()
                        header[1] = ((len shr 16) and 0xFF).toByte()
                        header[2] = ((len shr 8) and 0xFF).toByte()
                        header[3] = (len and 0xFF).toByte()
                        out.write(header)
                        out.write(nameBytes)
                        pipe(input, out, onProgress)
                        out.flush()
                    } ?: throw IllegalStateException("Unable to open input stream")
                } finally {
                    try { s.close() } catch (_: Throwable) {}
                }
            }
        }

        override fun close() {
            try { socket?.close() } catch (_: Throwable) {}
        }

        private fun pipe(input: InputStream, out: OutputStream, onProgress: (Int) -> Unit) {
            val buf = ByteArray(DEFAULT_BUFFER_SIZE)
            var total = 0L
            val expected = try { input.available().toLong() } catch (_: Throwable) { -1L }
            var read: Int
            var lastProgress = 0
            while (true) {
                read = input.read(buf)
                if (read == -1) break
                out.write(buf, 0, read)
                if (expected > 0) {
                    total += read
                    val p = ((total * 100) / expected).toInt().coerceIn(0, 100)
                    if (p != lastProgress) {
                        lastProgress = p
                        onProgress(p)
                    }
                }
            }
            if (expected <= 0) {
                onProgress(100)
            }
        }
    }

    private fun hasWifiP2pPermission(activity: Activity): Boolean {
        return if (android.os.Build.VERSION.SDK_INT >= 33) {
            activity.checkSelfPermission(android.Manifest.permission.NEARBY_WIFI_DEVICES) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
        } else {
            activity.checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
        }
    }
}
