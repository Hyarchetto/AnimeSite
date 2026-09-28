package com.liuyuxiang.animeserver.service;

import com.liuyuxiang.animeserver.dto.AdminAnimeItem;
import com.liuyuxiang.animeserver.dto.PageResult;
import com.liuyuxiang.animeserver.dto.AdminAnimeQuery;
import com.liuyuxiang.animeserver.dto.AnimeTagPair;
import com.liuyuxiang.animeserver.dto.SaveAnimeRequest;
import com.liuyuxiang.animeserver.exception.ApiException;
import com.liuyuxiang.animeserver.mapper.AnimeMapper;
import com.liuyuxiang.animeserver.mapper.AnimeTagMapper;
import com.liuyuxiang.animeserver.mapper.BannerMapper;
import com.liuyuxiang.animeserver.mapper.TagMapper;
import com.liuyuxiang.animeserver.util.LikeEscaper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 管理后台的番剧增删改查
 *
 * <p>和 AnimeService 分开，是为了让「公开读」和「管理员写」两个方向的代码不混在一起。
 * 两边都要用同一个 Mapper，那没问题，分层是按职责切的不是按表切的
 */
@Service
public class AdminAnimeService {

    private static final int MAX_TITLE_LENGTH = 200;
    private static final int MAX_COVER_LENGTH = 500;
    private static final int MAX_LATEST_LENGTH = 100;

    private final AnimeMapper animeMapper;
    private final BannerMapper bannerMapper;
    private final ImageService imageService;
    private final TagMapper tagMapper;
    private final AnimeTagMapper animeTagMapper;

    public AdminAnimeService(AnimeMapper animeMapper, BannerMapper bannerMapper, ImageService imageService,
                             TagMapper tagMapper, AnimeTagMapper animeTagMapper) {
        this.animeMapper = animeMapper;
        this.bannerMapper = bannerMapper;
        this.imageService = imageService;
        this.tagMapper = tagMapper;
        this.animeTagMapper = animeTagMapper;
    }

    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 100;
    private static final Set<String> SORTABLE_FIELDS = Set.of("title", "release_date");

    /**
     * 管理列表：筛选 + 排序 + 分页
     *
     * <p>筛选和排序都必须在这里做，不能交给前端。前端只能处理已经取回来的数据，
     * 一旦分了页，没取回来的那些就筛不到了
     */
    public PageResult<AdminAnimeItem> list(AdminAnimeQuery query) {
        String keyword = LikeEscaper.escape(query.getQ() == null ? "" : query.getQ().trim());

        // 排序字段白名单。传了不认识的字段就当没传，用默认排序——
        // 报错会让前端因为一个参数写错就整个列表打不开，不如静默回退
        //
        // 先判 null 再 contains，**不能把两次判断合成一句**：
        // Set.of 建出来的不可变集合，contains(null) 会直接抛空指针而不是返回 false
        String requested = query.getSort();
        String sort = requested != null && SORTABLE_FIELDS.contains(requested) ? requested : null;
        String order = "asc".equals(query.getOrder()) ? "asc" : "desc";

        int size = query.getSize() == null || query.getSize() < 1
                ? DEFAULT_PAGE_SIZE
                : Math.min(query.getSize(), MAX_PAGE_SIZE);
        int page = query.getPage() == null || query.getPage() < 1 ? 1 : query.getPage();

        long total = animeMapper.countForAdmin(keyword, query.getVisible());
        // 页码可能超出实际范围（比如翻到第 3 页之后又加了筛选条件）。
        // 不纠正的话会返回一个空列表，看着像「没有数据」
        int totalPages = Math.max(1, (int) ((total + size - 1) / size));
        page = Math.min(page, totalPages);

        List<AdminAnimeItem> items = animeMapper.selectPageForAdmin(
                keyword, query.getVisible(), sort, order, size, (page - 1) * size);
        attachTags(items);

        return new PageResult<>(items, total, page, size);
    }

    public AdminAnimeItem create(SaveAnimeRequest request) {
        AdminAnimeItem anime = new AdminAnimeItem();
        applyRequest(anime, request);

        animeMapper.insert(anime);
        replaceTags(anime.getId(), request.getTagIds());

        // 回读一次，让返回的对象带上后面才能确定的字段，前端拿到就能直接用
        return withTags(animeMapper.selectById(anime.getId()));
    }

    public AdminAnimeItem update(Long animeId, SaveAnimeRequest request) {
        AdminAnimeItem existing = animeMapper.selectById(animeId);
        if (existing == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "番剧不存在");
        }

        String oldCover = existing.getCover();

        AdminAnimeItem anime = new AdminAnimeItem();
        anime.setId(animeId);
        applyRequest(anime, request);
        animeMapper.update(anime);
        replaceTags(animeId, request.getTagIds());

        // 换过封面的话，旧图可能没人用了。
        // 放在更新之后：先保证库里的记录是新的，再去动磁盘
        if (oldCover != null && !oldCover.equals(anime.getCover())) {
            imageService.deleteIfOrphaned(oldCover);
        }

        return withTags(animeMapper.selectById(animeId));
    }

    /** 一次全取关联再按番剧分组。番剧量小的时候最省事，数据量大要改成按页内的 id 过滤 */
    private void attachTags(List<AdminAnimeItem> items) {
        Map<Long, List<Long>> byAnime = animeTagMapper.selectAllPairs().stream()
                .collect(Collectors.groupingBy(AnimeTagPair::getAnimeId,
                        Collectors.mapping(AnimeTagPair::getTagId, Collectors.toList())));
        items.forEach(item -> item.setTagIds(byAnime.getOrDefault(item.getId(), List.of())));
    }

    private AdminAnimeItem withTags(AdminAnimeItem anime) {
        if (anime != null) {
            anime.setTagIds(animeTagMapper.selectPairsByAnime(anime.getId()).stream()
                    .map(AnimeTagPair::getTagId)
                    .toList());
        }
        return anime;
    }

    /**
     * 整组替换某部番的标签
     *
     * <p>**先校验再删。** 反过来的话，传进来一个不存在的标签 id 会先把原有的
     * 标签全删掉，然后插入失败——用户什么都没改却把标签弄丢了
     *
     * <p>去重是因为 (anime_id, tag_id) 是联合主键，同一个标签传两次会撞主键
     */
    private void replaceTags(Long animeId, List<Long> rawTagIds) {
        List<Long> tagIds = rawTagIds == null
                ? List.of()
                : rawTagIds.stream().filter(Objects::nonNull).distinct().toList();

        if (!tagIds.isEmpty() && tagMapper.selectExistingIds(tagIds).size() != tagIds.size()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "选中的标签里有已经不存在的，请刷新后重试");
        }

        animeTagMapper.deleteByAnime(animeId);
        tagIds.forEach(tagId -> animeTagMapper.insert(animeId, tagId));
    }

    /**
     * 删番剧，连同它的图片一起
     *
     * <p>顺序不能改：先取图片路径，再删行，最后删文件。行删掉之后路径就查不到了，
     * 而文件必须在数据库落定之后才动——见 ImageService 的说明
     *
     * <p>剧集、标签关联、轮播、所有用户的追番和观看记录都由外键级联清掉。
     * 这是不可逆的，管理员的误操作没法撤销
     */
    public void delete(Long animeId) {
        AdminAnimeItem anime = animeMapper.selectById(animeId);
        if (anime == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "番剧不存在");
        }

        List<String> images = new ArrayList<>();
        images.add(anime.getCover());
        images.addAll(bannerMapper.selectImageUrlsByAnime(animeId));

        animeMapper.delete(animeId);

        images.forEach(imageService::deleteIfOrphaned);
    }

    /**
     * 上架与下架
     *
     * <p>单独一个接口而不并进 update，是因为下架是高频的单点操作。
     * 并进全量更新的话，一次「只想下架」的请求如果漏传了 title，标题就被清空了
     */
    public void setVisibility(Long animeId, boolean visible) {
        if (animeMapper.updateVisibility(animeId, visible) == 0) {
            throw new ApiException(HttpStatus.NOT_FOUND, "番剧不存在");
        }
    }

    private void applyRequest(AdminAnimeItem anime, SaveAnimeRequest request) {
        String title = trim(request.getTitle());
        if (title.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "番剧名称不能为空");
        }
        if (title.length() > MAX_TITLE_LENGTH) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "番剧名称不能超过 " + MAX_TITLE_LENGTH + " 个字符");
        }

        String cover = trim(request.getCover());
        if (cover.length() > MAX_COVER_LENGTH) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "封面地址过长");
        }

        String latest = trim(request.getLatest());
        if (latest.length() > MAX_LATEST_LENGTH) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "更新状态不能超过 " + MAX_LATEST_LENGTH + " 个字符");
        }

        anime.setTitle(title);
        anime.setCover(cover.isEmpty() ? null : cover);
        anime.setLatest(latest.isEmpty() ? null : latest);
        anime.setReleaseDate(request.getReleaseDate());
        anime.setDesc(request.getDesc());
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
