package com.smartshare.app.domain.ai

import android.content.Context
import android.net.Uri
import com.smartshare.app.domain.model.FileItem

/**
 * PUBLIC_INTERFACE
 * AiCategorizer provides AI-powered categorization and suggestions.
 * This implementation uses local heuristics based on name, size, and pseudo-recentness.
 */
class AiCategorizer(private val context: Context) {

    /**
     * Suggests top files for quick sharing based on simple heuristics.
     */
    fun suggestQuickShare(): List<FileItem> {
        // Placeholder heuristic: return a mocked set as demonstration.
        return listOf(
            FileItem(Uri.parse("content://local/recent_photo"), "Recent Photo.jpg", "Photos", 800_000),
            FileItem(Uri.parse("content://local/recent_video"), "Meeting Clip.mp4", "Videos", 42_000_000),
            FileItem(Uri.parse("content://local/recent_audio"), "Voice Note.m4a", "Music", 2_000_000),
        )
    }

    /**
     * Categorizes file by extension.
     */
    fun categorizeByName(name: String): String {
        val lower = name.lowercase()
        return when {
            lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png") || lower.endsWith(".gif") -> "Photos"
            lower.endsWith(".mp4") || lower.endsWith(".mkv") || lower.endsWith(".mov") -> "Videos"
            lower.endsWith(".mp3") || lower.endsWith(".m4a") || lower.endsWith(".wav") -> "Music"
            lower.endsWith(".vcf") -> "Contacts"
            else -> "Files"
        }
    }
}
