package com.liuyuxiang.animeserver.dto;

import java.util.List;

/**
 * 详情页的番剧信息：在列表那套字段上多一个简介、一个上下架标记和一组标签
 *
 * <p>不复用 AdminAnimeItem。两者现在字段一样，但管理端往后一定会长出
 * 只有管理员才看得到的东西，到时候再拆就晚了
 */
public class AnimeDetail extends AnimeItem {

    private String desc;

    /**
     * 是否上架
     *
     * <p>**下架的番剧详情页仍然能打开**，因为用户可能从自己的追番或观看记录点进来。
     * 这个字段是给前端标「已下架」用的，不是用来拦访问的
     */
    private Boolean visible;

    /**
     * 这部番的标签，按名字升序。没有标签时是空列表不是 null
     *
     * <p>由 Service 单独查一次填进来，不在这条 SQL 里 JOIN——
     * JOIN 的话一部番有几个标签就返回几行，还得在 Java 里去重
     */
    private List<TagRef> tags;

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public Boolean getVisible() {
        return visible;
    }

    public void setVisible(Boolean visible) {
        this.visible = visible;
    }

    public List<TagRef> getTags() {
        return tags;
    }

    public void setTags(List<TagRef> tags) {
        this.tags = tags;
    }
}
