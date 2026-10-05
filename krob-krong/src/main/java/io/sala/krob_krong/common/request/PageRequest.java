package io.sala.krob_krong.common.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PageRequest {

    public static final int DEFAULT_SIZE = 20;
    public static final int MAX_SIZE = 100;

    private int page;
    private int size;

    /** Default: page 0, size 20. */
    public PageRequest() {
        this.page = 0;
        this.size = DEFAULT_SIZE;
    }

    public PageRequest(int page, int size) {
        if (page < 0) throw new IllegalArgumentException("page must be >= 0, got " + page);
        if (size < 1 || size > MAX_SIZE)
            throw new IllegalArgumentException("size must be 1-" + MAX_SIZE + ", got " + size);
        this.page = page;
        this.size = size;
    }

    public org.springframework.data.domain.PageRequest toSpring() {
        return org.springframework.data.domain.PageRequest.of(page, size);
    }

    public org.springframework.data.domain.PageRequest toSpring(org.springframework.data.domain.Sort sort) {
        return org.springframework.data.domain.PageRequest.of(page, size, sort);
    }
}
