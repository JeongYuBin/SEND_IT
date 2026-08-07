package com.sendit.share;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class AudioTranscriberTest {
    @Test
    void rejectsUnsafeAudioStorageKey() {
        AudioTranscriber transcriber = new AudioTranscriber(
                "./data/uploads", "whisper-cli", "./model.bin", 10, 1000);

        assertThatThrownBy(() -> transcriber.transcribe("../secret.wav"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
