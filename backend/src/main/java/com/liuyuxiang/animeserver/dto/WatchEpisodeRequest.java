package com.liuyuxiang.animeserver.dto;

/** POST /api/history 的请求体 */
public class WatchEpisodeRequest {

    private Long episodeId;

    public Long getEpisodeId() {
        return episodeId;
    }

    public void setEpisodeId(Long episodeId) {
        this.episodeId = episodeId;
    }
}
