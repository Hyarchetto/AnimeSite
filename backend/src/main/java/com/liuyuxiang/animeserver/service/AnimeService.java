package com.liuyuxiang.animeserver.service;

import com.liuyuxiang.animeserver.dto.AnimeDetail;
import com.liuyuxiang.animeserver.dto.AnimeItem;
import com.liuyuxiang.animeserver.dto.EpisodeItem;
import com.liuyuxiang.animeserver.exception.ApiException;
import com.liuyuxiang.animeserver.mapper.AnimeMapper;
import com.liuyuxiang.animeserver.mapper.EpisodeMapper;
import com.liuyuxiang.animeserver.mapper.TagMapper;
import com.liuyuxiang.animeserver.util.LikeEscaper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

/**
 * 番剧查询的业务层。
 *
 * <p>这里目前只是一层转发，没有任何加工逻辑 —— 保持它是因为 Controller 不直接调
 * Mapper 是约定俗成的分层：Controller 管 HTTP 语义，Service 管业务规则，
 * Mapper 管 SQL。将来要加缓存、权限、字段加工，位置就在这里。
 */
@Service
public class AnimeService {

    private final AnimeMapper animeMapper;
    private final EpisodeMapper episodeMapper;
    private final TagMapper tagMapper;

    /**
     * 构造器注入。只有一个构造器时 Spring 不需要 @Autowired 也能自动装配，
     * 这样字段可以是 final，依赖关系也一目了然。
     */
    public AnimeService(AnimeMapper animeMapper, EpisodeMapper episodeMapper, TagMapper tagMapper) {
        this.animeMapper = animeMapper;
        this.episodeMapper = episodeMapper;
        this.tagMapper = tagMapper;
    }

    /**
     * 公开列表
     *
     * <p>只返回上架的。下架的番剧在管理后台仍然看得到，见 AdminAnimeService
     */
    public List<AnimeItem> listVisible() {
        return animeMapper.selectVisible();
    }

    /**
     * 按名称和标签搜
     *
     * <p>两个条件都为空就是「全部上架的番剧」，所以番剧页浏览和搜索共用一个接口
     */
    public List<AnimeItem> search(String keyword, List<Long> tagIds) {
        String trimmed = keyword == null ? "" : keyword.trim();
        List<Long> ids = tagIds == null ? List.of() : tagIds.stream().filter(Objects::nonNull).toList();
        return animeMapper.search(LikeEscaper.escape(trimmed), ids);
    }

    /**
     * 详情
     *
     * <p>**下架的番剧也返回。** 用户可能从自己的追番或观看记录点进来，
     * 那两处下架的番剧仍然在。是不是上架由返回体里的 visible 字段带出去
     */
    public AnimeDetail detail(Long animeId) {
        AnimeDetail detail = animeMapper.selectDetailById(animeId);
        if (detail == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "番剧不存在");
        }
        detail.setTags(tagMapper.selectByAnime(animeId));
        return detail;
    }

    public List<EpisodeItem> episodes(Long animeId) {
        if (!animeMapper.existsById(animeId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "番剧不存在");
        }
        return episodeMapper.selectByAnime(animeId);
    }
}
