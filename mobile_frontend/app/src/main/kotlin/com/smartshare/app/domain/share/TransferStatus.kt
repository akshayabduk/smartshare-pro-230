package com.smartshare.app.domain.share

/**
 * PUBLIC_INTERFACE
 * TransferStatus describes progress and state of a transfer.
 */
data class TransferStatus(
    val id: Int,
    val displayName: String,
    val progress: Int,
    val state: String
)
