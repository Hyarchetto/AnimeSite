package com.liuyuxiang.animeserver.dto;

/**
 * anime_tag 关联表的一行，纯粹用来把「哪部番有哪些标签」查出来
 *
 * <p>不直接用 Map 是因为那样字段名是字符串，写错编译期发现不了
 */
public class AnimeTagPair {

    private Long animeId;

    private Long tagId;

    public Long getAnimeId() {
        return animeId;
    }

    public void setAnimeId(Long animeId) {
        this.animeId = animeId;
    }

    public Long getTagId() {
        return tagId;
    }

    public void setTagId(Long tagId) {
        this.tagId = tagId;
    }
}
