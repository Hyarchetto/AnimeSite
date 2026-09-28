package com.liuyuxiang.animeserver.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;

/**
 * GET /api/anime 的返回项
 *
 * <p>字段名必须与前端逐字一致。Home.vue 直接对返回数组排序再切片，AnimeCard.vue
 * 直接读 cover / title / latest
 */
public class AnimeItem {

    private Long id;

    private String title;

    /** 映射自数据库列 cover_image */
    private String cover;

    /** 映射自数据库列 status */
    private String latest;

    /**
     * 映射自数据库列 release_date
     *
     * <p>前端写的是 new Date(item.release_date)，所以 JSON 里必须保持 snake_case，
     * 不能让它按 Java 字段名序列化成 releaseDate
     */
    @JsonProperty("release_date")
    private LocalDate releaseDate;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getCover() {
        return cover;
    }

    public void setCover(String cover) {
        this.cover = cover;
    }

    public String getLatest() {
        return latest;
    }

    public void setLatest(String latest) {
        this.latest = latest;
    }

    public LocalDate getReleaseDate() {
        return releaseDate;
    }

    public void setReleaseDate(LocalDate releaseDate) {
        this.releaseDate = releaseDate;
    }
}
