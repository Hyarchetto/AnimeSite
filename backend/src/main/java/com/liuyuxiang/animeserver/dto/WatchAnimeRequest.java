package com.liuyuxiang.animeserver.dto;

/** POST /api/watchlist 的请求体 */
public class WatchAnimeRequest {

    private Long animeId;

    public Long getAnimeId() {
        return animeId;
    }

    public void setAnimeId(Long animeId) {
        this.animeId = animeId;
    }
}
