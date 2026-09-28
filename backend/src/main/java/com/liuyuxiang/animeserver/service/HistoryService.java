package com.liuyuxiang.animeserver.service;

import com.liuyuxiang.animeserver.dto.HistoryItem;
import com.liuyuxiang.animeserver.exception.ApiException;
import com.liuyuxiang.animeserver.mapper.EpisodeMapper;
import com.liuyuxiang.animeserver.mapper.HistoryMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 观看记录的业务层
 *
 * <p>**一部番一行**，记的是「看到第几集」。往后看、重看、手动改进度，
 * 走的都是同一行的更新，不会堆积出多条
 */
@Service
public class HistoryService {

    private final HistoryMapper historyMapper;
    private final EpisodeMapper episodeMapper;

    public HistoryService(HistoryMapper historyMapper, EpisodeMapper episodeMapper) {
        this.historyMapper = historyMapper;
        this.episodeMapper = episodeMapper;
    }

    public List<HistoryItem> list(Long userId) {
        return historyMapper.selectByUser(userId);
    }

    /** 从详情页点某一集时调用。集号由这一集推出，所以这里只用给剧集 id */
    public void record(Long userId, Long episodeId) {
        Long animeId = episodeMapper.findAnimeIdById(episodeId);
        if (animeId == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "剧集不存在");
        }
        historyMapper.upsert(userId, animeId, episodeId);
    }

    public void delete(Long userId, Long animeId) {
        historyMapper.delete(userId, animeId);
    }
}
