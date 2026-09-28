package com.liuyuxiang.animeserver.controller;

import com.liuyuxiang.animeserver.dto.EpisodeItem;
import com.liuyuxiang.animeserver.dto.SaveEpisodeRequest;
import com.liuyuxiang.animeserver.service.AdminEpisodeService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理后台的剧集管理
 *
 * <p>读取复用公开的 `GET /api/anime/{id}/episodes`——剧集没有上架下架的概念，
 * 管理员和普通用户看到的是同一份数据，没必要做两个接口
 *
 * <p>写操作的路径一个挂在番剧下（新建时要指定是哪部番），一个平铺在 episodes 下
 * （改删时剧集 id 本身就够了）
 */
@RestController
@RequestMapping("/api/admin")
public class AdminEpisodeController {

    private final AdminEpisodeService adminEpisodeService;

    public AdminEpisodeController(AdminEpisodeService adminEpisodeService) {
        this.adminEpisodeService = adminEpisodeService;
    }

    @PostMapping("/anime/{animeId}/episodes")
    public EpisodeItem create(@PathVariable Long animeId, @RequestBody SaveEpisodeRequest request) {
        return adminEpisodeService.create(animeId, request);
    }

    @PutMapping("/episodes/{id}")
    public EpisodeItem update(@PathVariable Long id, @RequestBody SaveEpisodeRequest request) {
        return adminEpisodeService.update(id, request);
    }

    @DeleteMapping("/episodes/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        adminEpisodeService.delete(id);
    }
}
