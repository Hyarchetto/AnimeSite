package com.liuyuxiang.animeserver.dto;

/**
 * 上传成功的返回
 *
 * <p>只回一个地址，不回文件名或大小——调用方要的只是「把它填进那个字段」。
 * 包成对象而不是直接回字符串，是为了以后要加字段时不用改响应形状
 *
 * <p>地址是 `/uploads/xxx.jpg` 这种**站内相对路径**，前端用 `resolveImageUrl`
 * 补上后端的源。库里存的也是这个形式，换端口不用改数据
 */
public class UploadResult {

    private final String url;

    public UploadResult(String url) {
        this.url = url;
    }

    public String getUrl() {
        return url;
    }
}
