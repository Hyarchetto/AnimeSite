package com.liuyuxiang.animeserver.dto;

/**
 * 管理端的轮播项
 *
 * <p>和公开的 BannerItem 不是一套形状。公开那个是给首页轮播组件渲染用的，
 * 只有标题、状态、简介和两张图；管理端要知道**关联的是哪部番**（编辑时要回填下拉框），
 * 还要知道排序和启用状态
 */
public class AdminBannerItem {

    private Long id;

    /** 关联的番剧 */
    private Long animeId;

    private String animeTitle;

    /** 番剧的竖版封面。横版大图单独上传，两张不是一回事 */
    private String animeCover;

    /** 轮播用的横版大图 */
    private String imageUrl;

    /** 是否启用。列名 is_active，开了下划线转驼峰要起别名才映射得上 */
    private Boolean active;

    /**
     * 关联的番剧是否上架
     *
     * <p>番剧下架时它的轮播也不显示在首页，但**这条轮播本身还是启用的**。
     * 带这个字段出来是让管理页能标出「为什么不显示」，不然管理员会以为开关坏了
     */
    private Boolean animeVisible;

    /**
     * 展示顺序
     *
     * <p>**界面上不显示这个数字**，只用来排序。管理员靠列表上的上下箭头调整，
     * 和相邻那条交换值，不用自己填
     */
    private Integer sortOrder;

    public Boolean getAnimeVisible() {
        return animeVisible;
    }

    public void setAnimeVisible(Boolean animeVisible) {
        this.animeVisible = animeVisible;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
}
