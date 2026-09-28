package com.liuyuxiang.animeserver.dto;

/**
 * 管理后台的番剧项：在公开的那套字段上多一个 visible
 *
 * <p>继承而不是另起一个类，是为了让后台拿到的字段名和公开接口完全一致，
 * 前端编辑表单能直接拿列表里的对象填
 *
 * <p>字段名是 visible 不是 isVisible。列名 is_visible 开了名字转换之后会变成
 * isvisible，和属性名对不上，所以 SQL 里一律写成 is_visible AS visible
 */
public class AdminAnimeItem extends AnimeItem {

    /** 是否在网页显示。只在管理后台可见 */
    private Boolean visible;

    /**
     * 简介
     *
     * <p>公开列表不带这个字段，因为它只在详情页和编辑表单里用得上，
     * 让列表接口背上整段文本没有意义
     */
    private String desc;

    /**
     * 打了哪些标签，只有 id
     *
     * <p>不带标签名，是为了让编辑表单能直接用它回填多选框。
     * 要显示名字的话前端拿 id 去标签列表里查一下就行
     */
    private java.util.List<Long> tagIds;

    public java.util.List<Long> getTagIds() {
        return tagIds;
    }

    public void setTagIds(java.util.List<Long> tagIds) {
        this.tagIds = tagIds;
    }

    public Boolean getVisible() {
        return visible;
    }

    public void setVisible(Boolean visible) {
        this.visible = visible;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }
}
