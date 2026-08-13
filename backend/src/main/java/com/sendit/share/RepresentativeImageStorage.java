package com.sendit.share;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Service
public class RepresentativeImageStorage implements AutoCloseable {
    private static final Pattern SAFE_IMAGE_KEY = Pattern.compile(
            "[0-9a-f-]+-frame-[0-9]{2}(?:-place-[1-4])?\\.jpg");
    private final Path localRoot;
    private final String bucket;
    private final S3Client s3;

    public RepresentativeImageStorage(
            @Value("${app.media.storage-directory}") String storageDirectory,
            @Value("${app.storage.s3-bucket:}") String bucket,
            @Value("${app.storage.s3-region:ap-northeast-2}") String region) {
        this.localRoot = Path.of(storageDirectory).toAbsolutePath().normalize();
        this.bucket = bucket == null ? "" : bucket.trim();
        this.s3 = this.bucket.isBlank() ? null
                : S3Client.builder().region(Region.of(region)).build();
    }

    public String persist(String localKey) {
        if (!SAFE_IMAGE_KEY.matcher(localKey).matches()) return null;
        Path source = localRoot.resolve(localKey).normalize();
        if (!source.startsWith(localRoot) || !Files.isRegularFile(source)) return null;
        if (s3 == null) return "/api/v1/media/" + localKey;
        String objectKey = objectKey(localKey);
        s3.putObject(PutObjectRequest.builder()
                        .bucket(bucket).key(objectKey).contentType("image/jpeg")
                        .cacheControl("public, max-age=31536000, immutable").build(),
                RequestBody.fromFile(source));
        try { Files.deleteIfExists(source); } catch (IOException ignored) { }
        return "/api/v1/media/" + localKey;
    }

    public byte[] read(String key) throws IOException {
        if (!SAFE_IMAGE_KEY.matcher(key).matches() || s3 == null) return null;
        try (var response = s3.getObject(GetObjectRequest.builder()
                .bucket(bucket).key(objectKey(key)).build())) {
            return response.readAllBytes();
        } catch (software.amazon.awssdk.services.s3.model.NoSuchKeyException exception) {
            return null;
        }
    }

    public boolean enabled() { return s3 != null; }

    private String objectKey(String key) { return "place-images/" + key; }

    @Override
    public void close() {
        if (s3 != null) s3.close();
    }
}
