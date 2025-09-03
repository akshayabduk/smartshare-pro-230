package com.smartshare.app.domain.model

import android.net.Uri

/**
 * PUBLIC_INTERFACE
 * FileItem represents a selectable file-like entity for sharing/preview.
 */
data class FileItem(
    val uri: Uri,
    val displayName: String,
    val category: String,
    val size: Long
)
