package com.sendit.share;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class CarouselPlaceImageService {
    private static final int GRID_SIZE = 4;
    private final Path storageRoot;
    private final RepresentativeImageStorage imageStorage;

    public CarouselPlaceImageService(
            @Value("${app.media.storage-directory}") String storageDirectory,
            RepresentativeImageStorage imageStorage) {
        this.storageRoot = Path.of(storageDirectory).toAbsolutePath().normalize();
        this.imageStorage = imageStorage;
    }

    public List<PageMetadata> attach(List<PageMetadata> places, List<String> frameKeys) {
        if (places.isEmpty() || frameKeys.size() < 2) return places;

        List<String> contentFrames = frameKeys.subList(1, frameKeys.size());
        int singleFrameCount = inferSingleFrameCount(places.size(), contentFrames.size());
        if (singleFrameCount < 0) return mapOneFramePerPlace(places, contentFrames);

        List<PageMetadata> result = new ArrayList<>(places.size());
        int placeIndex = 0;
        for (int frameIndex = 0; frameIndex < contentFrames.size()
                && placeIndex < places.size(); frameIndex++) {
            String frameKey = contentFrames.get(frameIndex);
            if (frameIndex < singleFrameCount) {
                result.add(withImage(places.get(placeIndex++), mediaUrl(frameKey)));
                continue;
            }
            List<String> crops = cropGrid(frameKey);
            for (String crop : crops) {
                if (placeIndex >= places.size()) break;
                result.add(withImage(places.get(placeIndex++), mediaUrl(crop)));
            }
        }
        while (placeIndex < places.size()) result.add(places.get(placeIndex++));
        return List.copyOf(result);
    }

    private int inferSingleFrameCount(int placeCount, int frameCount) {
        int numerator = GRID_SIZE * frameCount - placeCount;
        if (numerator < 0 || numerator % (GRID_SIZE - 1) != 0) return -1;
        int singles = numerator / (GRID_SIZE - 1);
        return singles <= frameCount ? singles : -1;
    }

    private List<PageMetadata> mapOneFramePerPlace(
            List<PageMetadata> places, List<String> frames) {
        List<PageMetadata> result = new ArrayList<>(places.size());
        for (int index = 0; index < places.size(); index++) {
            String image = index < frames.size() ? mediaUrl(frames.get(index)) : null;
            result.add(image == null ? places.get(index) : withImage(places.get(index), image));
        }
        return List.copyOf(result);
    }

    private List<String> cropGrid(String frameKey) {
        Path source = safePath(frameKey);
        if (source == null) return List.of(frameKey, frameKey, frameKey, frameKey);
        try {
            BufferedImage image = ImageIO.read(source.toFile());
            if (image == null) return List.of(frameKey, frameKey, frameKey, frameKey);
            int halfWidth = image.getWidth() / 2;
            int halfHeight = image.getHeight() / 2;
            List<String> keys = new ArrayList<>(GRID_SIZE);
            for (int index = 0; index < GRID_SIZE; index++) {
                int x = (index % 2) * halfWidth;
                int y = (index / 2) * halfHeight;
                int width = index % 2 == 0 ? halfWidth : image.getWidth() - x;
                int height = index / 2 == 0 ? halfHeight : image.getHeight() - y;
                String cropKey = cropKey(frameKey, index + 1);
                Path target = storageRoot.resolve(cropKey).normalize();
                if (target.startsWith(storageRoot)) {
                    ImageIO.write(image.getSubimage(x, y, width, height), "jpg", target.toFile());
                    keys.add(cropKey);
                }
            }
            return keys.size() == GRID_SIZE
                    ? List.copyOf(keys) : List.of(frameKey, frameKey, frameKey, frameKey);
        } catch (IOException exception) {
            return List.of(frameKey, frameKey, frameKey, frameKey);
        }
    }

    private Path safePath(String key) {
        Path path = storageRoot.resolve(key).normalize();
        return path.startsWith(storageRoot) && Files.isRegularFile(path) ? path : null;
    }

    private String cropKey(String frameKey, int position) {
        int extension = frameKey.lastIndexOf('.');
        String base = extension > 0 ? frameKey.substring(0, extension) : frameKey;
        return base + "-place-" + position + ".jpg";
    }

    private String mediaUrl(String key) {
        String persisted = imageStorage.persist(key);
        return persisted == null ? "/api/v1/media/" + key : persisted;
    }

    private PageMetadata withImage(PageMetadata place, String imageUrl) {
        return new PageMetadata(place.title(), place.description(), imageUrl,
                place.placeName(), place.category(), place.address(),
                place.latitude(), place.longitude());
    }
}
