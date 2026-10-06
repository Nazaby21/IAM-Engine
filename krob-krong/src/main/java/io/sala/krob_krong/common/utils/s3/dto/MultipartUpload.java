package io.sala.krob_krong.common.utils.s3.dto;

public record MultipartUpload(String key, String uploadId, long partSize) {}
