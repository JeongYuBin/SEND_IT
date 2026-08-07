package com.sendit.share;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AutomaticMediaDownloaderTest {

    @TempDir
    Path tempDir;

    @Test
    void supportsOnlyApprovedPublicSnsHttpsUrls() {
        var downloader = new AutomaticMediaDownloader(
                tempDir.toString(), "yt-dlp", 1024, 10);

        assertThat(downloader.supports("https://www.instagram.com/reel/example/")).isTrue();
        assertThat(downloader.supports("https://youtu.be/example")).isTrue();
        assertThat(downloader.supports(
                "https://www.tiktok.com/@creator/video/6718335390845095173")).isTrue();
        assertThat(downloader.supports("https://vm.tiktok.com/example/")).isTrue();
        assertThat(downloader.supports("http://www.instagram.com/reel/example/")).isFalse();
        assertThat(downloader.supports("https://instagram.com.evil.example/reel/example/")).isFalse();
        assertThat(downloader.supports("https://tiktok.com.evil.example/video/1")).isFalse();
        assertThat(downloader.supports("https://127.0.0.1/video")).isFalse();
    }
}
