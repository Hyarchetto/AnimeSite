package com.liuyuxiang.animeserver.dto;

import java.time.LocalDate;

/**
 * 新增与修改剧集的请求体
 *
 * <p>字段名和 EpisodeItem 的响应完全一致，前端编辑表单可以把列表里的对象
 * 原样改几个字段再提交
 *
 * <p>和 SaveAnimeRequest 不同，这里**不需要 @JsonProperty**。集号、播出日期、
 * 观看地址在响应里都是驼峰，正好就是 Java 属性名
 */
public class SaveEpisodeRequest {

    private Integer episodeNo;

    private String title;

    private LocalDate airDate;

    private String watchUrl;

    public Integer getEpisodeNo() {
        return episodeNo;
    }

    public void setEpisodeNo(Integer episodeNo) {
        this.episodeNo = episodeNo;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public LocalDate getAirDate() {
        return airDate;
    }

    public void setAirDate(LocalDate airDate) {
        this.airDate = airDate;
    }

    public String getWatchUrl() {
        return watchUrl;
    }

    public void setWatchUrl(String watchUrl) {
        this.watchUrl = watchUrl;
    }
}
