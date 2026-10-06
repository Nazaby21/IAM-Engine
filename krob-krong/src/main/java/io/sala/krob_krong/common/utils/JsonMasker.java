package io.sala.krob_krong.common.utils;

import io.sala.krob_krong.common.properties.MaskingConfig;
import java.util.Map;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;
import tools.jackson.databind.node.StringNode;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class JsonMasker {
    private static final ObjectMapper MAPPER = JsonMapper.builder().build();

    public static String mask(String json, MaskingConfig config) {
        if (StringUtils.isBlank(json)) return json;

        String truncated = truncate(json, config.getMaxBodyLogSize());

        try {
            JsonNode tree = MAPPER.readTree(truncated);
            maskNode(tree, config);
            return MAPPER.writeValueAsString(tree);
        } catch (Exception _) {
            return "[non-json] " + truncated;
        }
    }

    private static void maskNode(JsonNode node, MaskingConfig config) {
        if (node.isObject()) {
            ObjectNode obj = (ObjectNode) node;
            for (Map.Entry<String, JsonNode> entry : obj.properties()) {
                if (config.isSensitive(entry.getKey())) {
                    String original =
                            entry.getValue().isString() ? entry.getValue().asString() : null;
                    obj.set(entry.getKey(), new StringNode(config.maskValue(original)));
                } else {
                    maskNode(entry.getValue(), config);
                }
            }
        } else if (node.isArray()) {
            ArrayNode arr = (ArrayNode) node;
            for (int i = 0; i < arr.size(); i++) {
                maskNode(arr.get(i), config);
            }
        }
    }

    private static String truncate(String value, int maxLength) {
        if (maxLength > 0 && value.length() > maxLength) {
            return value.substring(0, maxLength) + "...[truncated]";
        }
        return value;
    }
}
