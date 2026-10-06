package io.sala.krob_krong.common.log;

import io.sala.krob_krong.common.properties.MaskingConfig;
import io.sala.krob_krong.common.utils.JsonMasker;
import io.sala.krob_krong.common.utils.XmlMasker;
import java.util.LinkedHashMap;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class MaskedLogger {

    private final Logger delegate;
    private final MaskingConfig config;

    public MaskedLogger(Logger delegate, MaskingConfig config) {
        this.delegate = delegate;
        this.config = config;
    }

    public static MaskedLogger of(Class<?> clazz) {
        return new MaskedLogger(LoggerFactory.getLogger(clazz), MaskingConfig.defaults());
    }

    public static MaskedLogger of(Class<?> clazz, MaskingConfig config) {
        return new MaskedLogger(LoggerFactory.getLogger(clazz), config);
    }

    // -----------------------------------------------------------------------
    // Masking helpers
    // -----------------------------------------------------------------------

    public String maskJson(String json) {
        return JsonMasker.mask(json, config);
    }

    public String maskXml(String xml) {
        return XmlMasker.mask(xml, config);
    }

    public Map<String, String> maskHeaders(Map<String, String> headers) {
        if (headers == null || headers.isEmpty()) return headers;
        Map<String, String> masked = LinkedHashMap.newLinkedHashMap(headers.size());
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            if (config.isSensitive(entry.getKey())) {
                masked.put(entry.getKey(), config.maskValue(entry.getValue()));
            } else {
                masked.put(entry.getKey(), entry.getValue());
            }
        }
        return masked;
    }

    public String maskBody(String body, String contentType) {
        if (StringUtils.isBlank(body)) return "";
        if (contentType != null) {
            String ct = contentType.toLowerCase();
            if (ct.contains("json")) return maskJson(body);
            if (ct.contains("xml")) return maskXml(body);
        }
        // Plain text — just truncate
        int max = config.getMaxBodyLogSize();
        if (max > 0 && body.length() > max) {
            return body.substring(0, max) + "...[truncated]";
        }
        return body;
    }

    // -----------------------------------------------------------------------
    // SLF4J delegation
    // -----------------------------------------------------------------------
    public void trace(String format, Object... args) {
        delegate.trace(format, args);
    }

    public void debug(String format, Object... args) {
        delegate.debug(format, args);
    }

    public void info(String format, Object... args) {
        delegate.info(format, args);
    }

    public void warn(String format, Object... args) {
        delegate.warn(format, args);
    }

    public void error(String format, Object... args) {
        delegate.error(format, args);
    }

    public boolean isTraceEnabled() {
        return delegate.isTraceEnabled();
    }

    public boolean isDebugEnabled() {
        return delegate.isDebugEnabled();
    }

    public boolean isInfoEnabled() {
        return delegate.isInfoEnabled();
    }

    public Logger unwrap() {
        return delegate;
    }

    public MaskingConfig config() {
        return config;
    }
}
