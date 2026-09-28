package com.liuyuxiang.animeserver.dto;

import java.time.LocalDateTime;

/**
 * GET /api/history 的返回项
 *
 * <p>继承 AnimeItem 是为了把番剧那部分字段原样带给前端，AnimeCard 拿到的形状
 * 和在首页、追番页完全一致，不用为观看记录页单独适配
 *
 * <p>新增的两个字段用驼峰，而继承来的 release_date 是下划线。
 * 一个对象里两种命名确实别扭，但 release_date 是前端已经依赖的既定契约改不得，
 * 新字段则没有理由跟着用下划线——那只会把这个历史包袱继续扩散到新代码里
 */
public class HistoryItem extends AnimeItem {

    /** 看到第几集 */
    private Integer episodeNo;

    private LocalDateTime watchedAt;

    public Integer getEpisodeNo() {
        return episodeNo;
    }

    public void setEpisodeNo(Integer episodeNo) {
        this.episodeNo = episodeNo;
    }

    public LocalDateTime getWatchedAt() {
        return watchedAt;
    }

    public void setWatchedAt(LocalDateTime watchedAt) {
        this.watchedAt = watchedAt;
    }
}
