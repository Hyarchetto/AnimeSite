package com.liuyuxiang.animeserver.service;

import com.liuyuxiang.animeserver.dto.AnimeItem;
import com.liuyuxiang.animeserver.exception.ApiException;
import com.liuyuxiang.animeserver.mapper.AnimeMapper;
import com.liuyuxiang.animeserver.mapper.WatchlistMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 追番的业务层
 *
 * <p>user_id 一律由调用方从 JWT 里带进来，绝不从请求体或路径取。
 * 从请求体取的话，改一个数字就能操作别人的收藏
 */
@Service
public class WatchlistService {

    private final WatchlistMapper watchlistMapper;
    private final AnimeMapper animeMapper;

    public WatchlistService(WatchlistMapper watchlistMapper, AnimeMapper animeMapper) {
        this.watchlistMapper = watchlistMapper;
        this.animeMapper = animeMapper;
    }

    public List<AnimeItem> list(Long userId) {
        return watchlistMapper.selectByUser(userId);
    }

    public void add(Long userId, Long animeId) {
        if (animeId == null || !animeMapper.existsById(animeId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "番剧不存在");
        }
        watchlistMapper.insert(userId, animeId);
    }

    public void remove(Long userId, Long animeId) {
        watchlistMapper.delete(userId, animeId);
    }
}
