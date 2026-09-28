package com.liuyuxiang.animeserver.dto;

/**
 * 发表评论的请求体
 *
 * <p>animeId 放在请求体里而不是路径上，是为了让发评论的路径能统一成
 * POST /api/comments。拦截器是按路径前缀配的，路径里带上番剧 id 的话，
 * 「读评论公开、发评论要登录」这个区别就没法用路径表达了
 */
public class PostCommentRequest {

    private Long animeId;

    private String content;

    public Long getAnimeId() {
        return animeId;
    }

    public void setAnimeId(Long animeId) {
        this.animeId = animeId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
