package com.smartshare.app.domain.share.transport

import android.app.Activity
import android.content.Context
import android.net.Uri
import androidx.annotation.MainThread

/**
 * PUBLIC_INTERFACE
 * ShareTransport defines a transport for device-to-device file transfer.
 * Implementations handle discovery, connection, and sending a content Uri.
 */
interface ShareTransport {
    /**
     * PUBLIC_INTERFACE
     * Begin device discovery and present a device chooser UI as needed.
     * The implementation should either present UI directly or invoke the provided callback
     * with a connected transport ready to send.
     *
     * onReady: called when a peer connection is established and ready to send.
     * onError: called if discovery or connection fails with a user displayable message.
     */
    @MainThread
    fun startDiscovery(activity: Activity, onReady: (PeerSession) -> Unit, onError: (String) -> Unit)
}

/**
 * PUBLIC_INTERFACE
 * PeerSession encapsulates a ready connection to a peer device over a specific transport.
 * Calling sendFile will transfer the content Uri to the connected peer.
 */
interface PeerSession {
    /**
     * PUBLIC_INTERFACE
     * Send a file to the connected peer.
     * uri: content Uri to stream
     * displayName: human-friendly name, may be sent out-of-band for UI
     */
    suspend fun sendFile(context: Context, uri: Uri, displayName: String, onProgress: (Int) -> Unit)
    /**
     * PUBLIC_INTERFACE
     * Close and cleanup the connection.
     */
    fun close()
}
