package com.liuyuxiang.animeserver.dto;

/** 新增或改名标签的请求体 */
public class SaveTagRequest {

    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
