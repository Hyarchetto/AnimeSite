package com.liuyuxiang.animeserver.service;

import com.liuyuxiang.animeserver.dto.BannerItem;
import com.liuyuxiang.animeserver.mapper.BannerMapper;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 首页轮播的业务层
 *
 * <p>这一层真正有活干 —— 兜底默认值放在这里，而不是写进 SQL 的 COALESCE，
 * 目的是让这一层承担职责，而不是一层空转发
 */
@Service
public class BannerService {

    private static final String DEFAULT_STATUS = "已完结";
    private static final String DEFAULT_DESC = "暂无简介";

    private final BannerMapper bannerMapper;

    public BannerService(BannerMapper bannerMapper) {
        this.bannerMapper = bannerMapper;
    }

    public List<BannerItem> listActive() {
        List<BannerItem> banners = bannerMapper.selectActive();
        for (BannerItem item : banners) {
            if (isNullOrEmpty(item.getStatus())) {
                item.setStatus(DEFAULT_STATUS);
            }
            if (isNullOrEmpty(item.getDesc())) {
                item.setDesc(DEFAULT_DESC);
            }
        }
        return banners;
    }

    /**
     * 空串同样要兜底，不能只判 null。数据库里存了空串的话，页面上会显示成一片空白
     *
     * <p>不做 trim，全是空格的字符串按有效值处理
     */
    private static boolean isNullOrEmpty(String value) {
        return value == null || value.isEmpty();
    }
}
