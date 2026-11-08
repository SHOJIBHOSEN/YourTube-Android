package com.yourtube.android.core.media

import java.net.URI
import java.util.Locale

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
            val uri = URI(value)
            val scheme = uri.scheme?.lowercase(Locale.ROOT)
            if (scheme != "http" && scheme != "https") return false
            val host = uri.host?.lowercase(Locale.ROOT) ?: return false
            if (host == "youtube.com" || host.endsWith(".youtube.com") ||
                host == "youtu.be" || host.endsWith(".youtu.be")) return false
            val path = uri.path.orEmpty()
            val extension = path.substringAfterLast('/', "")
                .substringAfterLast('.', "")
                .lowercase(Locale.ROOT)
            extension in supportedExtensions
        } catch (_: Exception) {
            false
        }
    }
}
