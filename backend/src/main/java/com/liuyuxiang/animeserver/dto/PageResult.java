package com.liuyuxiang.animeserver.dto;

import java.util.List;

/**
 * 分页结果
 *
 * <p>带 total 而不是只给个 hasMore，是因为前端要画页码——底部那排数字
 * 得先知道总共有几页
 *
 * <p>做成泛型是因为番剧列表和用户列表都要用。两个页面写两份一样的结构，
 * 以后改字段名就得改两处
 */
public class PageResult<T> {

    private final List<T> items;

    /** 符合筛选条件的总条数，不是当前页的条数 */
    private final long total;

    private final int page;

    private final int size;

    public PageResult(List<T> items, long total, int page, int size) {
        this.items = items;
        this.total = total;
        this.page = page;
        this.size = size;
    }

    /** 总页数。至少 1 页，空结果时也显示「第 1 页 / 共 1 页」而不是 0 页 */
    public int getTotalPages() {
        if (size <= 0) {
            return 1;
        }
        return Math.max(1, (int) ((total + size - 1) / size));
    }

    public List<T> getItems() {
        return items;
    }

    public long getTotal() {
        return total;
    }

    public int getPage() {
        return page;
    }

    public int getSize() {
        return size;
    }
}
