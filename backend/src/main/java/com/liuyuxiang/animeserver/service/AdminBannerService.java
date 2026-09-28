package com.liuyuxiang.animeserver.service;

import com.liuyuxiang.animeserver.dto.AdminBannerItem;
import com.liuyuxiang.animeserver.dto.SaveBannerRequest;
import com.liuyuxiang.animeserver.exception.ApiException;
import com.liuyuxiang.animeserver.mapper.AnimeMapper;
import com.liuyuxiang.animeserver.mapper.BannerMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 首页轮播的管理
 *
 * <p>轮播是「哪部番 + 一张横版大图 + 排序 + 是否启用」。
 * 那张横版图和番剧的竖版封面不是一回事，所以单独存 image_url
 */
@Service
public class AdminBannerService {

    private static final int MAX_URL_LENGTH = 500;

    private final BannerMapper bannerMapper;
    private final AnimeMapper animeMapper;
    private final ImageService imageService;

    public AdminBannerService(BannerMapper bannerMapper, AnimeMapper animeMapper,
                              ImageService imageService) {
        this.bannerMapper = bannerMapper;
        this.animeMapper = animeMapper;
        this.imageService = imageService;
    }

    public List<AdminBannerItem> list() {
        return bannerMapper.selectAllForAdmin();
    }

    public AdminBannerItem create(SaveBannerRequest request) {
        AdminBannerItem banner = new AdminBannerItem();
        applyRequest(banner, request, null);

        bannerMapper.insert(banner);
        return bannerMapper.selectById(banner.getId());
    }

    public AdminBannerItem update(Long bannerId, SaveBannerRequest request) {
        AdminBannerItem current = bannerMapper.selectById(bannerId);
        if (current == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "轮播不存在");
        }

        AdminBannerItem banner = new AdminBannerItem();
        banner.setId(bannerId);
        // 把当前这条传进去，没传的字段从它身上取
        applyRequest(banner, request, current);

        String oldImage = current.getImageUrl();
        bannerMapper.update(banner);

        // 换过图的话，旧图可能没人用了。放在更新之后：先保证库里的记录是新的，
        // 再去动磁盘。ImageService 会跳过预置图、也会查还有没有别的记录在引用
        if (oldImage != null && !oldImage.equals(banner.getImageUrl())) {
            imageService.deleteIfOrphaned(oldImage);
        }

        return bannerMapper.selectById(bannerId);
    }

    /**
     * 移除轮播
     *
     * <p>**只删这一条轮播，不删番剧**。磁盘上的图片留着——
     * 同一张图可能被别的记录引用，删文件由 ImageService 在删番剧时统一判断
     */
    public void delete(Long bannerId) {
        AdminBannerItem current = bannerMapper.selectById(bannerId);
        if (current == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "轮播不存在");
        }

        bannerMapper.delete(bannerId);

        // 这张图可能没人再用了。和换图一样，先删库行再动磁盘
        imageService.deleteIfOrphaned(current.getImageUrl());
    }

    /**
     * 把请求里的字段并到目标对象上
     *
     * @param current 当前库里的那条。新建时传 null，表示没有原值可沿
     *
     * <p>**每个字段都要定下来，一个都不能留在 null 上。**
     * 改的时候只填一半的话，UPDATE 会把没填的列写成 NULL——
     * anime_id 和 is_active 都是 NOT NULL，直接报错。
     * 而且这种错看着像「服务器坏了」，其实只是没传字段
     */
    private void applyRequest(AdminBannerItem banner, SaveBannerRequest request, AdminBannerItem current) {
        Long animeId = request.getAnimeId() != null
                ? request.getAnimeId()
                : (current == null ? null : current.getAnimeId());
        if (animeId == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "请选择要轮播的番剧");
        }
        // 只在校验新传进来的值。沿用原值不用再查一次——它本来就在库里
        if (request.getAnimeId() != null && !animeMapper.existsById(animeId)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "选中的番剧不存在");
        }
        banner.setAnimeId(animeId);

        String rawUrl = request.getImageUrl() != null
                ? request.getImageUrl().trim()
                : (current == null ? null : current.getImageUrl());
        if (rawUrl == null || rawUrl.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "请填写轮播图地址");
        }
        if (rawUrl.length() > MAX_URL_LENGTH) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "图片地址过长");
        }
        banner.setImageUrl(rawUrl);

        banner.setActive(request.getActive() != null
                ? request.getActive()
                : (current == null ? Boolean.TRUE : current.getActive()));

        // 交换排序时前端会传值进来；新建时不传就排到最后
        banner.setSortOrder(request.getSortOrder() != null
                ? request.getSortOrder()
                : (current == null ? nextSortOrder() : current.getSortOrder()));
    }

    /** 新建时默认排到最后。加新轮播一般是往后接，不是插到最前 */
    private int nextSortOrder() {
        return bannerMapper.selectAllForAdmin().stream()
                .map(AdminBannerItem::getSortOrder)
                .filter(java.util.Objects::nonNull)
                .max(Integer::compareTo)
                .orElse(0) + 1;
    }
}
