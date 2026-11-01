package com.ciyuan.common;

import lombok.Data;

import java.util.List;

@Data
public class PageResult<T> {
    private List<T> list;
    private long total;
    private long page;
    private long size;
    private long pages;

    public static <T> PageResult<T> of(List<T> list, long total, long page, long size) {
        PageResult<T> r = new PageResult<>();
        r.list = list;
        r.total = total;
        r.page = page;
        r.size = size;
        r.pages = size <= 0 ? 0 : (total + size - 1) / size;
        return r;
    }
}
