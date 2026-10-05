package io.sala.krob_krong.iam.config.security;

import java.io.Serializable;

public record DeviceInfo(String fingerprint, String ipAddress, String userAgent, String deviceId)
        implements Serializable {}
