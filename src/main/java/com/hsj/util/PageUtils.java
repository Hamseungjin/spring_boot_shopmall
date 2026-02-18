package com.hsj.util;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public final class PageUtils {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    private PageUtils() {
    }

    public static Pageable of(int page, int size) {
        return PageRequest.of(
                Math.max(page, DEFAULT_PAGE),
                Math.min(Math.max(size, 1), MAX_SIZE)
        );
    }

    public static Pageable of(int page, int size, Sort sort) {
        return PageRequest.of(
                Math.max(page, DEFAULT_PAGE),
                Math.min(Math.max(size, 1), MAX_SIZE),
                sort
        );
    }
}
