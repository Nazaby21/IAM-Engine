package io.sala.krob_krong.common.utils;

import io.sala.krob_krong.common.config.properties.MaskingConfig;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class XmlMasker {

    private static final Pattern ELEMENT_PATTERN =
            Pattern.compile("<([a-zA-Z][a-zA-Z0-9_.-]*)(?:\\s[^>]*)?>([^<]*)</\\1>");

    private static final ConcurrentHashMap<String, Pattern> ELEMENT_PATTERNS = new ConcurrentHashMap<>();

    public static String mask(String xml, MaskingConfig config) {
        if (StringUtils.isBlank(xml)) return xml;

        String truncated = truncate(xml, config.getMaxBodyLogSize());

        if (!truncated.contains("<")) {
            return "[non-xml] " + truncated;
        }

        try {
            String result = truncated;
            result = maskBySensitiveFields(result, config);
            result = maskBySensitivePatterns(result, config);
            return result;
        } catch (Exception _) {
            return "[non-xml] " + truncated;
        }
    }

    private static String maskBySensitiveFields(String xml, MaskingConfig config) {
        String result = xml;
        for (String field : config.getSensitiveFields()) {
            result = maskElement(result, field, config);
        }
        return result;
    }

    private static String maskBySensitivePatterns(String xml, MaskingConfig config) {
        Matcher matcher = ELEMENT_PATTERN.matcher(xml);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            String elementName = matcher.group(1);
            if (!config.getSensitiveFields().contains(elementName.toLowerCase()) && config.isSensitive(elementName)) {
                String masked = config.maskValue(matcher.group(2));
                matcher.appendReplacement(
                        sb, "<" + elementName + ">" + Matcher.quoteReplacement(masked) + "</" + elementName + ">");
            } else {
                matcher.appendReplacement(sb, Matcher.quoteReplacement(matcher.group()));
            }
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private static String maskElement(String xml, String elementName, MaskingConfig config) {
        Pattern p = ELEMENT_PATTERNS.computeIfAbsent(elementName, XmlMasker::compileElementPattern);
        Matcher m = p.matcher(xml);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            String masked = config.maskValue(m.group(2));
            m.appendReplacement(sb, Matcher.quoteReplacement(m.group(1) + masked + m.group(3)));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private static Pattern compileElementPattern(String elementName) {
        return Pattern.compile(
                "(<" + Pattern.quote(elementName) + "(?:\\s[^>]*)?>)([^<]*?)(</" + Pattern.quote(elementName) + ">)",
                Pattern.CASE_INSENSITIVE);
    }

    private static String truncate(String value, int maxLength) {
        if (maxLength > 0 && value.length() > maxLength) {
            return value.substring(0, maxLength) + "...[truncated]";
        }
        return value;
    }
}
