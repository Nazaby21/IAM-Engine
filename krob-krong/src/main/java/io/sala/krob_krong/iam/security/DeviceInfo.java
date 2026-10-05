package io.sala.krob_krong.iam.security;

import java.io.Serializable;

public record DeviceInfo(String fingerprint, String ipAddress, String userAgent, String deviceId)
        implements Serializable {}
