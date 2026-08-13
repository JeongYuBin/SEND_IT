package com.sendit.share;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/media")
public class MediaController {
    private final Path storageRoot;
    private final RepresentativeImageStorage imageStorage;

    public MediaController(@Value("${app.media.storage-directory}") String storageDirectory,
            RepresentativeImageStorage imageStorage) {
        this.storageRoot = Path.of(storageDirectory).toAbsolutePath().normalize();
        this.imageStorage = imageStorage;
    }

    @GetMapping("/{key:[a-zA-Z0-9._-]+}")
    public ResponseEntity<Resource> get(@PathVariable String key) throws IOException {
        Path path = storageRoot.resolve(key).normalize();
        if (!path.startsWith(storageRoot)) return ResponseEntity.notFound().build();
        if (!Files.isRegularFile(path)) {
            byte[] stored = imageStorage.read(key);
            if (stored == null) return ResponseEntity.notFound().build();
            return ResponseEntity.ok()
                    .cacheControl(CacheControl.maxAge(java.time.Duration.ofDays(365)).cachePublic())
                    .contentType(MediaType.IMAGE_JPEG)
                    .body(new org.springframework.core.io.ByteArrayResource(stored));
        }
        String contentType = Files.probeContentType(path);
        Resource resource = new UrlResource(path.toUri());
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noCache())
                .header(HttpHeaders.CONTENT_TYPE,
                        contentType == null ? MediaType.IMAGE_JPEG_VALUE : contentType)
                .body(resource);
    }
}
