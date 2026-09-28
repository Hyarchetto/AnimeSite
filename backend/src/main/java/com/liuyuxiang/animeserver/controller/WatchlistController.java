package com.liuyuxiang.animeserver.controller;

import com.liuyuxiang.animeserver.dto.AnimeItem;
import com.liuyuxiang.animeserver.dto.WatchAnimeRequest;
import com.liuyuxiang.animeserver.service.WatchlistService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 我的追番
 *
 * <p>整个类都在 JwtInterceptor 的路径表里，userId 由它放好
 */
@RestController
@RequestMapping("/api/watchlist")
public class WatchlistController {

    private final WatchlistService watchlistService;

    public WatchlistController(WatchlistService watchlistService) {
        this.watchlistService = watchlistService;
    }

    @GetMapping
    public List<AnimeItem> list(@RequestAttribute("userId") Long userId) {
        return watchlistService.list(userId);
    }

    /** 返回 204 而不是把新列表带回来。加追番是一个动作，列表由前端决定要不要重取 */
    @PostMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void add(@RequestAttribute("userId") Long userId, @RequestBody WatchAnimeRequest request) {
        watchlistService.add(userId, request.getAnimeId());
    }

    @DeleteMapping("/{animeId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(@RequestAttribute("userId") Long userId, @PathVariable Long animeId) {
        watchlistService.remove(userId, animeId);
    }
}
