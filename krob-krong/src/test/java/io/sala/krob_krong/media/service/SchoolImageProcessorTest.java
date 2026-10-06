package io.sala.krob_krong.media.service;

import static org.assertj.core.api.Assertions.*;

import io.sala.krob_krong.common.exceptions.KrobKrongException;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class SchoolImageProcessorTest {
    private final SchoolImageProcessor processor = new SchoolImageProcessor();

    @Test
    void detectsActualFormatAndProducesVerifiedMetadata() throws Exception {
        byte[] png = png();
        var image = processor.process(new MockMultipartFile("file", "logo.jpg", "image/jpeg", png));
        assertThat(image.mimeType()).isEqualTo("image/png");
        assertThat(image.width()).isEqualTo(8);
        assertThat(image.height()).isEqualTo(4);
        assertThat(image.sha256()).isEqualTo(SchoolImageProcessor.hash(image.bytes()));
    }

    @Test
    void rejectsTextSvgAndEmptyFilesEvenWhenLabelledAsImages() {
        for (String data : new String[] {"", "<svg xmlns='http://www.w3.org/2000/svg'/>", "not an image"})
            assertThatThrownBy(() ->
                            processor.process(new MockMultipartFile("file", "logo.png", "image/png", data.getBytes())))
                    .isInstanceOf(KrobKrongException.class);
    }

    @Test
    void rejectsHugePixelDimensionsBeforeDecoding() throws Exception {
        byte[] png = png();
        ByteBuffer.wrap(png, 16, 4).putInt(9000);
        assertThatThrownBy(() -> processor.process(new MockMultipartFile("file", png)))
                .isInstanceOf(KrobKrongException.class);
    }

    @Test
    void rejectsFilesAboveUploadLimit() {
        assertThatThrownBy(() -> processor.process(
                        new MockMultipartFile("file", new byte[SchoolImageProcessor.MAX_INPUT_BYTES + 1])))
                .isInstanceOfSatisfying(
                        KrobKrongException.class, e -> assertThat(e.errorCode()).isEqualTo("IMAGE_TOO_LARGE"));
    }

    private static byte[] png() throws Exception {
        var output = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(8, 4, BufferedImage.TYPE_INT_RGB), "png", output);
        return output.toByteArray();
    }
}
