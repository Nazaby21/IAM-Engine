package io.sala.krob_krong.common.exceptions;

public interface ExceptionMapper<E extends Throwable> {

    /** The exception type this mapper handles. Used for type-safe dispatch. */
    Class<E> supportedType();

    /**
     * Translate the source exception. Return {@code null} if this mapper
     * decides not to handle the supplied instance (e.g. only some subtypes).
     */
    KrobKrongException map(E exception);

    /**
     * Lower values are tried first. Defaults to {@code 0} — frameworks ship
     * built-in mappers at higher values so app-supplied mappers win by default.
     */
    default int order() {
        return 0;
    }
}
