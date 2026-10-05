package io.sala.krob_krong.common.exceptions;

import io.sala.krob_krong.common.errors.ApiError;
import io.sala.krob_krong.common.errors.CommonErrorCode;
import io.sala.krob_krong.common.errors.ErrorCategory;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public final class ExceptionTranslator {

    private final List<ExceptionMapper<?>> mappers;
    private final boolean exposeUnknownMessage;

    public ExceptionTranslator(List<ExceptionMapper<?>> mappers, boolean exposeUnknownMessage) {
        this.mappers = mappers.stream()
                .sorted(Comparator.comparingInt(ExceptionMapper::order))
                .toList();
        this.exposeUnknownMessage = exposeUnknownMessage;
    }

    public KrobKrongException toKrobKrongException(Throwable t) {
        if (t instanceof KrobKrongException acm) return acm;
        for (ExceptionMapper<?> mapper : mappers) {
            KrobKrongException mapped = applyMapper(mapper, t);
            if (mapped != null) return mapped;
        }
        return null;
    }

    public ApiError toApiError(Throwable t) {
        KrobKrongException mapped = toKrobKrongException(t);
        if (mapped != null) return buildApiError(mapped);
        return new ApiError(
                CommonErrorCode.INTERNAL_ERROR.code(),
                exposeUnknownMessage && t.getMessage() != null
                        ? t.getMessage()
                        : CommonErrorCode.INTERNAL_ERROR.defaultMessage(),
                ErrorCategory.INTERNAL,
                null);
    }

    public static ApiError buildApiError(KrobKrongException ex) {
        Map<String, Object> ctx = ex.context();
        return new ApiError(
                ex.errorCode(),
                ex.getMessage(),
                ex.category(),
                (ctx == null || ctx.isEmpty()) ? null : Map.copyOf(ctx));
    }

    @SuppressWarnings("unchecked")
    private static <E extends Throwable> KrobKrongException applyMapper(ExceptionMapper<E> mapper, Throwable t) {
        if (!mapper.supportedType().isInstance(t)) return null;
        return mapper.map((E) t);
    }
}
