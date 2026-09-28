package com.liuyuxiang.animeserver.controller;

import com.liuyuxiang.animeserver.dto.AdminBannerItem;
import com.liuyuxiang.animeserver.dto.SaveBannerRequest;
import com.liuyuxiang.animeserver.service.AdminBannerService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 首页轮播的管理
 *
 * <p>列表**含未启用的**，否则管理员没法把停用的再打开
 */
@RestController
@RequestMapping("/api/admin/banners")
public class AdminBannerController {

    private final AdminBannerService bannerService;

    public AdminBannerController(AdminBannerService bannerService) {
        this.bannerService = bannerService;
    }

    @GetMapping
    public List<AdminBannerItem> list() {
        return bannerService.list();
    }

    @PostMapping
    public AdminBannerItem create(@RequestBody SaveBannerRequest request) {
        return bannerService.create(request);
    }

    /** 改的时候只传要改的字段就行，没传的沿用原值 */
    @PutMapping("/{id}")
    public AdminBannerItem update(@PathVariable Long id, @RequestBody SaveBannerRequest request) {
        return bannerService.update(id, request);
    }

    /** 只删这一条轮播，番剧和图片都不动 */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        bannerService.delete(id);
    }
}
