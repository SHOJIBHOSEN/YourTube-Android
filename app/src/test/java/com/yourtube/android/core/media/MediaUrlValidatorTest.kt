package com.yourtube.android.core.media

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaUrlValidatorTest {
    @Test
    fun acceptsSupportedDirectMediaExtensionsAndQueryParameters() {
        listOf(
            "https://cdn.example.com/video.mp4",
            "https://cdn.example.com/video.m4v?token=abc",
            "https://cdn.example.com/video.webm",
            "https://cdn.example.com/live/index.m3u8?sig=123",
            "https://cdn.example.com/manifest.mpd"
        ).forEach { assertTrue("Expected supported: $it", MediaUrlValidator.isSupported(it)) }
    }

    @Test
    fun rejectsMalformedUrlsAndUnsafeSchemes() {
        listOf(
            "", "   ", "not a url",
            "file:///sdcard/video.mp4",
            "content://media/video.mp4",
            "javascript:alert(1)",
            "https:///video.mp4"
        ).forEach { assertFalse("Expected rejected: $it", MediaUrlValidator.isSupported(it)) }
    }

    @Test
    fun rejectsYoutubeWatchAndShortsLinksAsDirectMedia() {
        listOf(
            "https://youtube.com/watch?v=abc123",
            "https://www.youtube.com/shorts/abc123",
            "https://youtu.be/abc123",
            "https://m.youtube.com/watch?v=abc123"
        ).forEach { assertFalse("Expected rejected: $it", MediaUrlValidator.isSupported(it)) }
    }

    @Test
    fun rejectsUnsupportedExtensions() {
        listOf(
            "https://cdn.example.com/page.html",
            "https://cdn.example.com/image.jpg",
            "https://cdn.example.com/video",
            "https://cdn.example.com/video.avi"
        ).forEach { assertFalse("Expected rejected: $it", MediaUrlValidator.isSupported(it)) }
    }

    @Test
    fun acceptsCaseInsensitiveSchemeAndExtension() {
        assertTrue(MediaUrlValidator.isSupported("HTTPS://cdn.example.com/MOVIE.MP4"))
    }
}
