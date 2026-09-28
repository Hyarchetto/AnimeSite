package com.liuyuxiang.animeserver.controller;

import com.liuyuxiang.animeserver.dto.HistoryItem;
import com.liuyuxiang.animeserver.dto.WatchEpisodeRequest;
import com.liuyuxiang.animeserver.service.HistoryService;
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
 * 观看记录
 *
 * <p>返回的列表按番剧去过重，同一部番只出现最近看的那一集
 */
@RestController
@RequestMapping("/api/history")
public class HistoryController {

    private final HistoryService historyService;

    public HistoryController(HistoryService historyService) {
        this.historyService = historyService;
    }

    @GetMapping
    public List<HistoryItem> list(@RequestAttribute("userId") Long userId) {
        return historyService.list(userId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void record(@RequestAttribute("userId") Long userId, @RequestBody WatchEpisodeRequest request) {
        historyService.record(userId, request.getEpisodeId());
    }

    /** 删记录。按番剧定位——一条记录对应一部番 */
    @DeleteMapping("/{animeId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@RequestAttribute("userId") Long userId, @PathVariable Long animeId) {
        historyService.delete(userId, animeId);
    }
}
