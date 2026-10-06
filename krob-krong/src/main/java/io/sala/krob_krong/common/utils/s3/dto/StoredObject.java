package io.sala.krob_krong.common.utils.s3.dto;

import java.time.Instant;

public record StoredObject(String key, String eTag, long size, String contentType, Instant lastModified) {}
