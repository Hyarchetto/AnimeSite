package com.liuyuxiang.animeserver.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;

/**
 * 新增与修改番剧的请求体
 *
 * <p>字段名沿用响应那套 cover / latest / release_date，前端编辑表单可以把
 * 列表里拿到的对象原样改几个字段再提交，不用做一次重命名映射
 *
 * <p>没有 visible。上下架走单独的接口，理由见 AdminAnimeService
 */
public class SaveAnimeRequest {

    private String title;

    private String cover;

    private String latest;

    /**
     * 必须标 @JsonProperty，否则收不到值
     *
     * <p>Jackson 默认按 Java 属性名也就是 releaseDate 匹配请求体里的键，
     * 前端按响应那套发过来的 release_date 会被静默丢掉，字段落库是 null，
     * 而且不报错
     */
    @JsonProperty("release_date")
    private LocalDate releaseDate;

    private String desc;

    /**
     * 要打的标签 id
     *
     * <p>**整组替换**的语义：传什么就是最终有哪些，没传或传空数组表示清空。
     * 不传（null）和传空数组在这里是一回事——标签列表本来就该由表单完整给出
     */
    private java.util.List<Long> tagIds;

    public java.util.List<Long> getTagIds() {
        return tagIds;
    }

    public void setTagIds(java.util.List<Long> tagIds) {
        this.tagIds = tagIds;
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

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }
}
