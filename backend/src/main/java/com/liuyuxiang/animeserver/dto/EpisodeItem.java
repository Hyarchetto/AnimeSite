package com.liuyuxiang.animeserver.dto;

import java.time.LocalDate;

/**
 * 详情页选集里的一项
 *
 * <p>watch_url 是外链，点开是去别的站点看。和封面那种站内相对路径不是一回事，
 * 所以不做任何地址转换，前端直接 window.open
 */
public class EpisodeItem {

    private Long id;

    /** 集号，从 1 开始 */
    private Integer episodeNo;

    private String title;

    private LocalDate airDate;

    private String watchUrl;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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
