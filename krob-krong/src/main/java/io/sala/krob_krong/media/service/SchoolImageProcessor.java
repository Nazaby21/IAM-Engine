package io.sala.krob_krong.media.service;

import io.sala.krob_krong.common.exceptions.BusinessException;
import io.sala.krob_krong.iam.error.IAMErrorCode;
import java.awt.image.BufferedImage;
import java.io.*;
import java.security.*;
import java.util.*;
import javax.imageio.*;
import javax.imageio.stream.MemoryCacheImageInputStream;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class SchoolImageProcessor {
    public static final int MAX_INPUT_BYTES = 10 * 1024 * 1024;
    public static final int MAX_STORED_BYTES = 20 * 1024 * 1024;

    public record Image(byte[] bytes, String mimeType, int width, int height, String sha256) {}

    public Image process(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new BusinessException(IAMErrorCode.INVALID_IMAGE);
        if (file.getSize() > MAX_INPUT_BYTES) throw new BusinessException(IAMErrorCode.IMAGE_TOO_LARGE);
        try (var input = file.getInputStream()) {
            byte[] source = input.readNBytes(MAX_INPUT_BYTES + 1);
            if (source.length > MAX_INPUT_BYTES) throw new BusinessException(IAMErrorCode.IMAGE_TOO_LARGE);
            try (var stream = new MemoryCacheImageInputStream(new ByteArrayInputStream(source))) {
                var readers = ImageIO.getImageReaders(stream);
                if (!readers.hasNext()) throw new BusinessException(IAMErrorCode.INVALID_IMAGE);
                ImageReader reader = readers.next();
                try {
                    String format = reader.getFormatName().toLowerCase(Locale.ROOT);
                    if (!Set.of("jpeg", "jpg", "png").contains(format))
                        throw new BusinessException(IAMErrorCode.INVALID_IMAGE);
                    reader.setInput(stream, true, true);
                    int width = reader.getWidth(0), height = reader.getHeight(0);
                    if (width < 1 || height < 1 || width > 8192 || height > 8192 || (long) width * height > 12_000_000)
                        throw new BusinessException(IAMErrorCode.INVALID_IMAGE);
                    BufferedImage image = reader.read(0);
                    if (image == null) throw new BusinessException(IAMErrorCode.INVALID_IMAGE);
                    // Re-encoding validates pixel data and discards uploaded metadata/trailing payloads.
                    var output = new ByteArrayOutputStream();
                    String target = format.equals("png") ? "png" : "jpeg";
                    if (!ImageIO.write(image, target, output)) throw new BusinessException(IAMErrorCode.INVALID_IMAGE);
                    byte[] bytes = output.toByteArray();
                    if (bytes.length > MAX_STORED_BYTES) throw new BusinessException(IAMErrorCode.IMAGE_TOO_LARGE);
                    return new Image(bytes, "image/" + target, width, height, hash(bytes));
                } finally {
                    reader.dispose();
                }
            }
        } catch (IOException | IllegalArgumentException ex) {
            throw new BusinessException(IAMErrorCode.INVALID_IMAGE);
        }
    }

    public static String hash(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
