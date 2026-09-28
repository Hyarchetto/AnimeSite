package com.liuyuxiang.animeserver.dto;

/**
 * 用户列表的查询条件
 *
 * <p>字段名和请求参数一一对应，Spring 会自动绑上去
 */
public class AdminUserQuery {

    /** 账号或用户名关键词，空表示不筛 */
    private String q;

    private Integer page;

    private Integer size;

    public String getQ() {
        return q;
    }

    public void setQ(String q) {
        this.q = q;
    }

    public Integer getPage() {
        return page;
    }

    public void setPage(Integer page) {
        this.page = page;
    }

    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }
}
