package com.liuyuxiang.animeserver.controller;

import com.liuyuxiang.animeserver.dto.AdminAnimeItem;
import com.liuyuxiang.animeserver.dto.PageResult;
import com.liuyuxiang.animeserver.dto.AdminAnimeQuery;
import com.liuyuxiang.animeserver.dto.SaveAnimeRequest;
import com.liuyuxiang.animeserver.dto.VisibilityRequest;
import com.liuyuxiang.animeserver.exception.ApiException;
import com.liuyuxiang.animeserver.service.AdminAnimeService;
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

/**
 * 管理后台的番剧管理
 *
 * <p>整个类在 AdminInterceptor 的保护下，够不到这里的请求说明不是管理员
 */
@RestController
@RequestMapping("/api/admin/anime")
public class AdminAnimeController {

    private final AdminAnimeService adminAnimeService;

    public AdminAnimeController(AdminAnimeService adminAnimeService) {
        this.adminAnimeService = adminAnimeService;
    }

    /**
     * 番剧列表，含已下架的，否则管理员没法把它们重新上架
     *
     * <p>query 是个普通对象，Spring 会从查询参数自动绑上去，
     * 不用逐个写 @RequestParam。参数：`q` 名称关键词、`visible` 上架状态、
     * `sort` + `order` 排序、`page` + `size` 分页
     */
    @GetMapping
    public PageResult<AdminAnimeItem> list(AdminAnimeQuery query) {
        return adminAnimeService.list(query);
    }

    @PostMapping
    public AdminAnimeItem create(@RequestBody SaveAnimeRequest request) {
        return adminAnimeService.create(request);
    }

    @PutMapping("/{id}")
    public AdminAnimeItem update(@PathVariable Long id, @RequestBody SaveAnimeRequest request) {
        return adminAnimeService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        adminAnimeService.delete(id);
    }

    /**
     * 上架与下架
     *
     * <p>和 update 分开，见 AdminAnimeService 的说明
     */
    @PutMapping("/{id}/visibility")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void setVisibility(@PathVariable Long id, @RequestBody VisibilityRequest request) {
        if (request.getVisible() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "缺少 visible");
        }
        adminAnimeService.setVisibility(id, request.getVisible());
    }
}
