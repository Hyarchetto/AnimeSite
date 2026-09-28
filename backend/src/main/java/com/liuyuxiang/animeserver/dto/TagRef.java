package com.liuyuxiang.animeserver.dto;

/**
 * 番剧详情页上展示的一个标签，只有 id 和名字
 *
 * <p>不复用 TagItem。那个带 animeCount，是搜索页和标签管理页要的计数，
 * 在详情页里没有意义，带上只会多一个恒为 null 的字段
 */
public class TagRef {

    private Long id;

    private String name;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
