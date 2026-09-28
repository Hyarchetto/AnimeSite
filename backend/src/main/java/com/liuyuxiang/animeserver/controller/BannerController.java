package com.liuyuxiang.animeserver.controller;

import com.liuyuxiang.animeserver.dto.BannerItem;
import com.liuyuxiang.animeserver.service.BannerService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 首页轮播接口。
 */
@RestController
@RequestMapping("/api")
public class BannerController {

    private final BannerService bannerService;

    public BannerController(BannerService bannerService) {
        this.bannerService = bannerService;
    }

    /** 同样返回裸数组，理由见 AnimeController */
    @GetMapping("/banner")
    public List<BannerItem> list() {
        return bannerService.listActive();
    }
}
