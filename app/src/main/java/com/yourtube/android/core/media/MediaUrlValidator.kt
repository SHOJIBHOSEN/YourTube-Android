package com.yourtube.android.core.media

import android.net.Uri

/**
 * Conservative classifier for user-authorized direct media URLs.
 * A YouTube watch/share URL is intentionally not a direct media source.
 */
object MediaUrlValidator {
    private val supportedExtensions = setOf("mp4", "m4v", "webm", "m3u8", "mpd")

    fun isSupported(raw: String): Boolean {
        val value = raw.trim()
        if (value.isEmpty()) return false
        return try {
            val uri = Uri.parse(value)
            val scheme = uri.scheme?.lowercase()
            if (scheme != "http" && scheme != "https") return false
            val host = uri.host?.lowercase() ?: return false
            if (host == "youtube.com" || host.endsWith(".youtube.com") ||
                host == "youtu.be" || host.endsWith(".youtu.be")) return false
            val extension = uri.lastPathSegment.orEmpty()
                .substringAfterLast('.', "")
                .lowercase()
            extension in supportedExtensions
        } catch (_: Exception) {
            false
        }
    }
}
