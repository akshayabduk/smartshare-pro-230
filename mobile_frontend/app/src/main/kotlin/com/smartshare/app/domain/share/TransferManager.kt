package com.smartshare.app.domain.share

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.smartshare.app.domain.model.FileItem
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * PUBLIC_INTERFACE
 * TransferManager enqueues share operations and exposes real-time status via LiveData.
 * This version simulates transfers; replace simulateTransfer with actual transport logic.
 */
class TransferManager private constructor(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val statusesInternal = MutableLiveData<List<TransferStatus>>(emptyList())
    private val statusMap = ConcurrentHashMap<Int, TransferStatus>()
    private val idGen = AtomicInteger(1)

    val statuses: LiveData<List<TransferStatus>> = statusesInternal

    /**
     * PUBLIC_INTERFACE
     * Enqueue a new share transfer for a FileItem.
     */
    fun enqueueShare(item: FileItem) {
        val id = idGen.getAndIncrement()
        val status = TransferStatus(id, item.displayName, 0, "Queued")
        statusMap[id] = status
        publish()
        scope.launch { simulateTransfer(id) }
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
