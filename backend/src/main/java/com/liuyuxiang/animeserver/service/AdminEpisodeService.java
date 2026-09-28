package com.liuyuxiang.animeserver.service;

import com.liuyuxiang.animeserver.dto.EpisodeItem;
import com.liuyuxiang.animeserver.dto.SaveEpisodeRequest;
import com.liuyuxiang.animeserver.exception.ApiException;
import com.liuyuxiang.animeserver.mapper.AnimeMapper;
import com.liuyuxiang.animeserver.mapper.EpisodeMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/**
 * 管理后台的剧集增删改
 *
 * <p>查询复用公开的那个接口，剧集没有「上架下架」这回事，
 * 番剧下架了它的剧集自然就跟着不见了
 */
@Service
public class AdminEpisodeService {

    private static final int MAX_TITLE_LENGTH = 200;
    private static final int MAX_URL_LENGTH = 500;

    private final EpisodeMapper episodeMapper;
    private final AnimeMapper animeMapper;

    public AdminEpisodeService(EpisodeMapper episodeMapper, AnimeMapper animeMapper) {
        this.episodeMapper = episodeMapper;
        this.animeMapper = animeMapper;
    }

    public EpisodeItem create(Long animeId, SaveEpisodeRequest request) {
        if (!animeMapper.existsById(animeId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "番剧不存在");
        }

        EpisodeItem episode = new EpisodeItem();
        applyRequest(episode, request);

        // 同一部番里集号唯一由数据库保证。这里不做先查再插的预检，
        // 两个人同时录同一集时那次预检会双双通过
        boolean taken = episodeMapper.existsByAnimeAndNo(animeId, episode.getEpisodeNo());
        if (taken) {
            throw new ApiException(HttpStatus.CONFLICT, "第 " + episode.getEpisodeNo() + " 集已经有了");
        }

        try {
            episodeMapper.insert(animeId, episode);
        } catch (DuplicateKeyException ex) {
            throw new ApiException(HttpStatus.CONFLICT, "第 " + episode.getEpisodeNo() + " 集已经有了");
        }

        return episodeMapper.selectById(episode.getId());
    }

    public EpisodeItem update(Long episodeId, SaveEpisodeRequest request) {
        EpisodeItem existing = episodeMapper.selectById(episodeId);
        if (existing == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "剧集不存在");
        }

        EpisodeItem episode = new EpisodeItem();
        episode.setId(episodeId);
        applyRequest(episode, request);

        try {
            episodeMapper.update(episode);
        } catch (DuplicateKeyException ex) {
            // 改集号时撞上同番里已有的那一集
            throw new ApiException(HttpStatus.CONFLICT, "第 " + episode.getEpisodeNo() + " 集已经有了");
        }

        return episodeMapper.selectById(episodeId);
    }

    /**
     * 删剧集
     *
     * <p>用户的观看记录引用着它，外键会把那些记录一起清掉——
     * 集都没了，记录留着也只是个悬空的进度
     */
    public void delete(Long episodeId) {
        if (episodeMapper.delete(episodeId) == 0) {
            throw new ApiException(HttpStatus.NOT_FOUND, "剧集不存在");
        }
    }

    private void applyRequest(EpisodeItem episode, SaveEpisodeRequest request) {
        Integer episodeNo = request.getEpisodeNo();
        if (episodeNo == null || episodeNo < 1) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "集号必须是从 1 开始的整数");
        }

        String title = request.getTitle() == null ? "" : request.getTitle().trim();
        if (title.length() > MAX_TITLE_LENGTH) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "单集标题不能超过 " + MAX_TITLE_LENGTH + " 个字符");
        }

        String watchUrl = request.getWatchUrl() == null ? "" : request.getWatchUrl().trim();
        if (watchUrl.length() > MAX_URL_LENGTH) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "观看地址过长");
        }

        episode.setEpisodeNo(episodeNo);
        episode.setTitle(title.isEmpty() ? null : title);
        episode.setAirDate(request.getAirDate());
        episode.setWatchUrl(watchUrl.isEmpty() ? null : watchUrl);
    }
}
