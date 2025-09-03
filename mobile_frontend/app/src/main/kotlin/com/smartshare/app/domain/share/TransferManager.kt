package com.smartshare.app.domain.share

import android.app.Activity
import android.content.Context
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.smartshare.app.domain.model.FileItem
import com.smartshare.app.domain.share.transport.BluetoothShareTransport
import com.smartshare.app.domain.share.transport.PeerSession
import com.smartshare.app.domain.share.transport.ShareTransport
import com.smartshare.app.domain.share.transport.WifiDirectShareTransport
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * PUBLIC_INTERFACE
 * TransferManager enqueues share operations and exposes real-time status via LiveData.
 * This version supports simulated cloud transfers and real device-to-device transfers via
 * Bluetooth and Wi‑Fi Direct using the ShareTransport abstraction.
 */
class TransferManager private constructor(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val statusesInternal = MutableLiveData<List<TransferStatus>>(emptyList())
    private val statusMap = ConcurrentHashMap<Int, TransferStatus>()
    private val idGen = AtomicInteger(1)

    val statuses: LiveData<List<TransferStatus>> = statusesInternal

    /**
     * PUBLIC_INTERFACE
     * Enqueue a new share transfer for a FileItem using simulated cloud (legacy behavior).
     */
    fun enqueueShare(item: FileItem) {
        val id = idGen.getAndIncrement()
        val status = TransferStatus(id, item.displayName, 0, "Queued (Cloud)")
        statusMap[id] = status
        publish()
        scope.launch { simulateTransfer(id) }
    }

    /**
     * PUBLIC_INTERFACE
     * Enqueue a direct device-to-device transfer via a selected transport.
     * transport: "bluetooth" | "wifi" for Wi‑Fi Direct
     */
    fun enqueueDirectShare(activity: Activity, item: FileItem, transport: String) {
        val id = idGen.getAndIncrement()
        statusMap[id] = TransferStatus(id, item.displayName, 0, "Discovering ($transport)")
        publish()

        val shareTransport: ShareTransport = when (transport.lowercase()) {
            "bluetooth" -> BluetoothShareTransport(activity)
            "wifi" -> WifiDirectShareTransport(activity)
            else -> {
                // Fallback to simulated if unknown
                scope.launch { simulateTransfer(id) }
                return
            }
        }

        activity.runOnUiThread {
            shareTransport.startDiscovery(
                activity,
                onReady = { session ->
                    scope.launch {
                        updateState(id, 1, "Connecting")
                        doSend(session, item.uri, item.displayName, id)
                    }
                },
                onError = { message ->
                    updateState(id, 0, "Error: $message")
                }
            )
        }
    }

    private suspend fun doSend(session: PeerSession, uri: Uri, displayName: String, id: Int) {
        try {
            session.sendFile(context, uri, displayName) { p ->
                statusMap[id]?.let { s ->
                    statusMap[id] = s.copy(progress = p.coerceIn(0, 100), state = "Transferring")
                    publish()
                }
            }
            updateState(id, 100, "Completed")
        } catch (t: Throwable) {
            updateState(id, 0, "Failed: ${t.message}")
        } finally {
            try { session.close() } catch (_: Throwable) {}
        }
    }

    private fun updateState(id: Int, progress: Int, state: String) {
        statusMap[id]?.let { s ->
            statusMap[id] = s.copy(progress = progress, state = state)
            publish()
        }
    }

    private suspend fun simulateTransfer(id: Int) {
        repeat(20) {
            delay(200)
            val s = statusMap[id] ?: return
            statusMap[id] = s.copy(progress = (s.progress + 5).coerceAtMost(100), state = if (s.progress >= 95) "Finalizing" else "In Progress")
            publish()
        }
        val s = statusMap[id] ?: return
        statusMap[id] = s.copy(progress = 100, state = "Completed")
        publish()
    }

    private fun publish() {
        statusesInternal.postValue(statusMap.values.sortedBy { it.id })
    }

    companion object {
        @Volatile private var INSTANCE: TransferManager? = null

        // PUBLIC_INTERFACE
        fun getInstance(context: Context): TransferManager =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: TransferManager(context.applicationContext).also { INSTANCE = it }
            }
    }
}
