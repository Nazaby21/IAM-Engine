package io.sala.krob_krong.common.utils.s3;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ETags {

    public static String unquote(String eTag) {
        if (eTag == null) {
            return null;
        }
        String trimmed = eTag.strip();
        return trimmed.length() >= 2 && trimmed.startsWith("\"") && trimmed.endsWith("\"")
                ? trimmed.substring(1, trimmed.length() - 1)
                : trimmed;
    }

    public static String quote(String eTag) {
        return "\"" + unquote(eTag) + "\"";
    }

    public static byte[] md5(byte[] content) {
        try {
            return MessageDigest.getInstance("MD5").digest(content);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("MD5 unavailable", e);
        }
    }

    public static String contentMd5(byte[] md5) {
        return Base64.getEncoder().encodeToString(md5);
    }

    public static String hex(byte[] md5) {
        return HexFormat.of().formatHex(md5);
    }

    // S3 multipart ETag: MD5 of the concatenated binary part MD5s, suffixed with the part count.
    public static String multipart(List<byte[]> partMd5s) {
        byte[] concatenated = new byte[partMd5s.size() * 16];
        for (int i = 0; i < partMd5s.size(); i++) {
            System.arraycopy(partMd5s.get(i), 0, concatenated, i * 16, 16);
        }
        return hex(md5(concatenated)) + "-" + partMd5s.size();
    }
}
