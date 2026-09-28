package com.liuyuxiang.animeserver.dto;

/**
 * GET /api/banner 的返回项
 *
 * <p>这个接口用驼峰 imageSrc / coverSrc，与 /api/anime 的下划线 release_date 不一致，
 * 前端 Banner.vue 按驼峰读，不做统一
 */
public class BannerItem {

    private String title;

    private String status;

    /** 映射自 anime 表的 desc 列，该列名是 MySQL 保留字，SQL 里必须反引号 */
    private String desc;

    /** 映射自 banner_poster.image_url */
    private String imageSrc;

    /** 映射自 anime.cover_image */
    private String coverSrc;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public String getImageSrc() {
        return imageSrc;
    }

    public void setImageSrc(String imageSrc) {
        this.imageSrc = imageSrc;
    }

    public String getCoverSrc() {
        return coverSrc;
    }

    public void setCoverSrc(String coverSrc) {
        this.coverSrc = coverSrc;
    }
}
