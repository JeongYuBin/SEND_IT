package com.sendit.share;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.List;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CarouselPlaceImageServiceTest {
    @TempDir Path storage;

    @Test
    void mapsSingleSlidesAndFourPlaceGridToDistinctImages() throws Exception {
        write("cover.jpg");
        write("single.jpg");
        write("grid.jpg");
        var imageStorage = new RepresentativeImageStorage(storage.toString(), "", "ap-northeast-2");
        var service = new CarouselPlaceImageService(storage.toString(), imageStorage);
        List<PageMetadata> places = List.of(
                place("한 곳"), place("둘"), place("셋"), place("넷"), place("다섯"));

        List<PageMetadata> result = service.attach(
                places, List.of("cover.jpg", "single.jpg", "grid.jpg"));

        assertThat(result).extracting(PageMetadata::imageUrl).doesNotHaveDuplicates();
        assertThat(result.getFirst().imageUrl()).endsWith("single.jpg");
        assertThat(result.get(1).imageUrl()).endsWith("grid-place-1.jpg");
        assertThat(storage.resolve("grid-place-4.jpg")).exists();
    }

    private void write(String name) throws Exception {
        BufferedImage image = new BufferedImage(200, 200, BufferedImage.TYPE_INT_RGB);
        var graphics = image.createGraphics();
        graphics.setColor(Color.GREEN);
        graphics.fillRect(0, 0, 200, 200);
        graphics.dispose();
        ImageIO.write(image, "jpg", storage.resolve(name).toFile());
    }

    private PageMetadata place(String name) {
        return new PageMetadata(null, null, null, name, "음식점", null, null, null);
    }
}
