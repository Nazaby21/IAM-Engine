package io.sala.krob_krong.common.utils.s3.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record PresignedPart(
        int partNumber, String url, String method, Map<String, List<String>> headers, Instant expiresAt) {}
