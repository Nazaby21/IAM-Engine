package io.sala.krob_krong.common.response;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.domain.Page;

@Getter
@Setter
@AllArgsConstructor
public class PageResponse<T> {

    private List<T> content;
    private PageMeta meta;

    public static <T> PageResponse<T> from(Page<T> springPage) {
        return new PageResponse<>(
                springPage.getContent(),
                PageMeta.builder()
                        .totalElements(springPage.getTotalElements())
                        .totalPages(springPage.getTotalPages())
                        .page(springPage.getNumber())
                        .size(springPage.getSize())
                        .hasNext(springPage.hasNext())
                        .build());
    }
}
