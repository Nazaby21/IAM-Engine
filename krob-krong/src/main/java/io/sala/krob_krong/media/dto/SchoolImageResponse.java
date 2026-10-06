package io.sala.krob_krong.media.dto;

import jakarta.validation.constraints.*;
import java.util.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SchoolImageResponse {
    private UUID id;
    private UUID schoolId;
    private String purpose;
    private String status;
    private String mimeType;
    private long byteSize;
    private int width;
    private int height;
    private String sha256;
    private String contentUrl;
}
