package com.yourtube.android.core.media

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaUrlValidatorTest {
    @Test fun acceptsDirectMediaWithQueryParameters() {
        assertTrue(MediaUrlValidator.isSupported("https://cdn.example/video.mp4?token=abc"))
        assertTrue(MediaUrlValidator.isSupported("https://cdn.example/live/index.m3u8?sig=123"))
    }

    @Test fun rejectsYouTubeWatchLinksAsDirectMedia() {
        assertFalse(MediaUrlValidator.isSupported("https://www.youtube.com/watch?v=abc123"))
        assertFalse(MediaUrlValidator.isSupported("https://youtu.be/abc123"))
    }

    @Test fun rejectsUnsafeOrUnsupportedUrls() {
        assertFalse(MediaUrlValidator.isSupported("file:///sdcard/video.mp4"))
        assertFalse(MediaUrlValidator.isSupported("https://example.com/page"))
        assertFalse(MediaUrlValidator.isSupported(""))
    }
}
