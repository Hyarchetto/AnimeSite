package com.liuyuxiang.animeserver.controller;

import com.liuyuxiang.animeserver.dto.TagItem;
import com.liuyuxiang.animeserver.service.AdminTagService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 公开的标签列表
 *
 * <p>搜索页要用它渲染标签选择区，**不需要登录**。
 * 管理端的增删改在 AdminTagController 里，走 /api/admin/tags
 */
@RestController
@RequestMapping("/api/tags")
public class TagController {

    private final AdminTagService tagService;

    public TagController(AdminTagService tagService) {
        this.tagService = tagService;
    }

    /**
     * 计数只算**上架的**番剧，和搜索页只搜得到上架的番保持一致。
     * 用全部番剧去数的话，标签上写着 3 点进去只有 2 条
     */
    @GetMapping
    public List<TagItem> list() {
        return tagService.listVisible();
    }
}
