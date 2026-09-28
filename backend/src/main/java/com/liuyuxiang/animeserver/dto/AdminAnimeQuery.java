package com.liuyuxiang.animeserver.dto;

/**
 * 番剧管理列表的查询条件
 *
 * <p>字段名和请求参数一一对应，Spring 会自动从 `?q=&visible=&sort=&order=&page=&size=` 绑上来，
 * 控制器里不用逐个写 @RequestParam
 *
 * <p>**刻意不做默认值以外的校验。** 排序字段和页码的合法性由 Service 归一化——
 * 放在这里的话，非法值要到查询时才炸，错误位置离原因太远
 */
public class AdminAnimeQuery {

    /** 名称关键词，空表示不按名称筛 */
    private String q;

    /** 是否上架。null 表示不按上架状态筛，三种状态就靠这个三态表达 */
    private Boolean visible;

    /** 排序字段：title / release_date。null 表示用默认排序 */
    private String sort;

    /** 排序方向：asc / desc */
    private String order;

    private Integer page;

    private Integer size;

    public String getQ() {
        return q;
    }

    public void setQ(String q) {
        this.q = q;
    }

    public Boolean getVisible() {
        return visible;
    }

    public void setVisible(Boolean visible) {
        this.visible = visible;
    }

    public String getSort() {
        return sort;
    }

    public void setSort(String sort) {
        this.sort = sort;
    }

    public String getOrder() {
        return order;
    }

    public void setOrder(String order) {
        this.order = order;
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
