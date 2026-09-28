package com.liuyuxiang.animeserver.controller;

import com.liuyuxiang.animeserver.dto.SaveTagRequest;
import com.liuyuxiang.animeserver.dto.TagItem;
import com.liuyuxiang.animeserver.service.AdminTagService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 标签管理。整个类在 AdminInterceptor 的保护下 */
@RestController
@RequestMapping("/api/admin/tags")
public class AdminTagController {

    private final AdminTagService tagService;

    public AdminTagController(AdminTagService tagService) {
        this.tagService = tagService;
    }

    /**
     * 列表里带每个标签被几部番用着，管理员删之前能知道会影响什么
     *
     * <p>**计数含已下架的番剧**，和公开的 `/api/tags` 不一样。
     * 管理员要的是「有多少部番挂着这个标签」，下架的也算
     */
    @GetMapping
    public List<TagItem> list() {
        return tagService.listAll();
    }

    @PostMapping
    public TagItem create(@RequestBody SaveTagRequest request) {
        return tagService.create(request);
    }

    @PutMapping("/{id}")
    public TagItem rename(@PathVariable Long id, @RequestBody SaveTagRequest request) {
        return tagService.rename(id, request);
    }

    /** 删标签不会删番剧，只是那些番不再有这个标签 */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        tagService.delete(id);
    }
}
