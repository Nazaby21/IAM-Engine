package io.sala.krob_krong.common.utils.s3.dto;

public record UploadedPart(int partNumber, String eTag, long size) {}
