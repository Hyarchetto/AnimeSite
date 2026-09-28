package com.liuyuxiang.animeserver.dto;

/**
 * 一个标签
 *
 * <p>带 animeCount 是因为管理页要显示「这个标签被几部番用着」——
 * 删标签之前得知道会影响什么
 */
public class TagItem {

    private Long id;

    private String name;

    /** 打了这个标签的番剧数 */
    private Integer animeCount;

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

    public Integer getAnimeCount() {
        return animeCount;
    }

    public void setAnimeCount(Integer animeCount) {
        this.animeCount = animeCount;
    }
}
