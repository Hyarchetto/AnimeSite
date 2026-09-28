package com.liuyuxiang.animeserver.controller;

import com.liuyuxiang.animeserver.dto.UploadResult;
import com.liuyuxiang.animeserver.service.ImageService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 管理员上传图片
 *
 * <p>**只上传，不应用。** 拿到地址之后由调用方填进表单，等表单整体保存时才生效。
 * 番剧封面、轮播大图都是这个流程——它们要等管理员把别的字段也填好
 *
 * <p>和 `POST /api/auth/avatar` 的区别就在这里：头像传完立刻生效，
 * 因为那是用户自己的一张图，没有别的字段要一起提交
 *
 * <p>校验和落盘都在 ImageService 里，两个入口共用同一套：
 * 扩展名白名单、大小上限、文件名重新生成
 */
@RestController
@RequestMapping("/api/admin")
public class AdminUploadController {

    private final ImageService imageService;

    public AdminUploadController(ImageService imageService) {
        this.imageService = imageService;
    }

    @PostMapping("/upload")
    public UploadResult upload(@RequestParam("file") MultipartFile file) {
        return new UploadResult(imageService.store(file));
    }
}
