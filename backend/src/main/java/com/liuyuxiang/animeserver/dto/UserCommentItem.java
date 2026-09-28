package com.liuyuxiang.animeserver.dto;

/**
 * 用户资料页上「TA 发过的评论」里的一条
 *
 * <p>在评论本体的基础上带上番剧信息，这样列表里能显示「在《某某》下评论」，
 * 也方便点回那部番的详情页。不带的话就只有一段没头没尾的话
 *
 * <p>继承的 nickname / avatar 在这个场景下是冗余的——整个列表都是同一个人发的。
 * 留着是因为复用现成的评论结构比另起一个更省事，多两个字段不值得拆
 */
public class UserCommentItem extends CommentItem {

    private Long animeId;

    private String animeTitle;

    private String animeCover;

    public Long getAnimeId() {
        return animeId;
    }

    public void setAnimeId(Long animeId) {
        this.animeId = animeId;
    }

    public String getAnimeTitle() {
        return animeTitle;
    }

    public void setAnimeTitle(String animeTitle) {
        this.animeTitle = animeTitle;
    }

    public String getAnimeCover() {
        return animeCover;
    }

    public void setAnimeCover(String animeCover) {
        this.animeCover = animeCover;
    }
}
