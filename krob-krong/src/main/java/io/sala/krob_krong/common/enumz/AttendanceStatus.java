package io.sala.krob_krong.common.enumz;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum AttendanceStatus {
    PRESENT("PRESENT"),
    ABSENT("ABSENT"),
    LATE("LATE"),
    PERMISSION("PERMISSION");

    private final String value;
}
