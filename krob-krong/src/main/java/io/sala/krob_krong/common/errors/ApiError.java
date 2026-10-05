package io.sala.krob_krong.common.errors;

import java.util.LinkedHashMap;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class ApiError {
    private String code;

    private String message;

    private ErrorCategory category;

    private Map<String, Object> details;

    public ApiError(String code, String message) {
        this(code, message, null, null);
    }

    public static ApiError of(String code, String message, ErrorCategory category) {
        return new ApiError(code, message, category, null);
    }

    public ApiError withDetail(String key, Object value) {
        if (key == null) return this;
        if (this.details == null) this.details = new LinkedHashMap<>();
        this.details.put(key, value);
        return this;
    }
}
