package com.smartshare.app.domain.share.transport

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

/**
 * PUBLIC_INTERFACE
 * BluetoothShareTransport discovers Bluetooth Classic devices and attempts an RFCOMM connection
 * to a selected device. This is a simplified reference that assumes the remote device runs a compatible
 * server socket. In production, implement both server and client roles and a service record UUID.
 */
class BluetoothShareTransport(private val context: Context) : ShareTransport {

    // Sample UUID for SPP-like service. In production define a fixed known UUID shared by clients/servers.
    private val serviceUuid: UUID = UUID.fromString("9a52c18d-9b26-4f2c-9d5f-5c1f0af0f9a2")

    override fun startDiscovery(activity: Activity, onReady: (PeerSession) -> Unit, onError: (String) -> Unit) {
        val adapter = getAdapter() ?: run {
            onError("Bluetooth not supported")
            return
        }
        if (!adapter.isEnabled) {
            onError("Bluetooth is disabled")
            return
        }

        // Check required permissions prior to bondedDevices access
        val hasPermission = hasBluetoothPermission(activity)
        if (!hasPermission) {
            onError("Bluetooth permission not granted")
            return
        }

        // Show system device picker if available, otherwise use bonded devices as a simple chooser.
        // For brevity, choose first bonded device; replace with proper UI list.
        val bonded = try {
            adapter.bondedDevices
        } catch (se: SecurityException) {
            onError("Missing Bluetooth permission")
            return
        }

        if (bonded.isNullOrEmpty()) {
            onError("No bonded devices. Pair a device in system settings.")
            activity.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
            return
        }
        // Pick the first for demo. In a full app, show a selection dialog.
        val target = bonded.first()
        onReady(BluetoothPeerSession(target, serviceUuid))
    }

    private fun getAdapter(): BluetoothAdapter? {
        val mgr = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager?
        return mgr?.adapter
    }

    private fun hasBluetoothPermission(activity: Activity): Boolean {
        return if (Build.VERSION.SDK_INT >= 31) {
            activity.checkSelfPermission(android.Manifest.permission.BLUETOOTH_CONNECT) == android.content.pm.PackageManager.PERMISSION_GRANTED
        } else {
            // Prior to Android 12, bondedDevices access typically needs BLUETOOTH and sometimes location
            val pm = activity.packageManager
            val hasBt = pm.checkPermission(android.Manifest.permission.BLUETOOTH, activity.packageName) == android.content.pm.PackageManager.PERMISSION_GRANTED
            val hasAdmin = pm.checkPermission(android.Manifest.permission.BLUETOOTH_ADMIN, activity.packageName) == android.content.pm.PackageManager.PERMISSION_GRANTED
            // For discovery/location-derived access, FINE_LOCATION may be required; we check but do not mandate if bonded list is accessible.
            val hasLoc = activity.checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED
            hasBt && hasAdmin && (hasLoc || true)
        }
    }

    private class BluetoothPeerSession(
        private val device: BluetoothDevice,
        private val serviceUuid: UUID
    ) : PeerSession {

        private var socket: BluetoothSocket? = null

        override suspend fun sendFile(context: Context, uri: Uri, displayName: String, onProgress: (Int) -> Unit) {
            withContext(Dispatchers.IO) {
                // Permission check before Bluetooth socket operations
                val hasPerm = if (Build.VERSION.SDK_INT >= 31) {
                    context.checkSelfPermission(android.Manifest.permission.BLUETOOTH_CONNECT) ==
                            android.content.pm.PackageManager.PERMISSION_GRANTED
                } else {
                    true // older APIs rely on manifest-declared BLUETOOTH/ADMIN
                }
                if (!hasPerm) throw SecurityException("BLUETOOTH_CONNECT permission not granted")

                val sock: BluetoothSocket = try {
                    device.createRfcommSocketToServiceRecord(serviceUuid)
                } catch (se: SecurityException) {
                    throw SecurityException("Missing Bluetooth permission to create socket")
                }
                socket = sock
                try {
                    sock.connect()
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        val out: OutputStream = sock.outputStream
                        // Very simple header: filename length + filename bytes + file bytes
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
                    // keep connection for re-use if needed; here we close
                    try { sock.close() } catch (_: Throwable) {}
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
}
