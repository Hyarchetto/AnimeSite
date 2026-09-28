package com.liuyuxiang.animeserver.dto;

/** PUT /api/admin/anime/{id}/visibility 的请求体 */
public class VisibilityRequest {

    private Boolean visible;

    public Boolean getVisible() {
        return visible;
    }

    public void setVisible(Boolean visible) {
        this.visible = visible;
    }
}
