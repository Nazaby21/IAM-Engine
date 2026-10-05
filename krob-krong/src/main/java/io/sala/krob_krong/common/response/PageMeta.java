package io.sala.krob_krong.common.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class PageMeta {
    private long totalElements;
    private int totalPages;
    private int page;
    private int size;
    private boolean hasNext;
}
