package com.abada.engine.api;

import com.abada.engine.core.exception.ProcessEngineException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;

final class Pagination {

    static final String DEFAULT_PAGE_SIZE = "50";
    static final int MAX_PAGE_SIZE = 100;

    private Pagination() {
    }

    static Pageable request(int page, int size, Sort sort) {
        if (page < 0) {
            throw new ProcessEngineException("page must be greater than or equal to 0");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new ProcessEngineException("size must be between 1 and " + MAX_PAGE_SIZE);
        }
        return PageRequest.of(page, size, sort);
    }

    static HttpHeaders headers(Page<?> page) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Page", Integer.toString(page.getNumber()));
        headers.set("X-Page-Size", Integer.toString(page.getSize()));
        headers.set("X-Total-Count", Long.toString(page.getTotalElements()));
        headers.set("X-Total-Pages", Integer.toString(page.getTotalPages()));
        return headers;
    }
}
