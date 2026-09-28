package com.liuyuxiang.animeserver.controller;

import com.liuyuxiang.animeserver.dto.AnimeDetail;
import com.liuyuxiang.animeserver.dto.AnimeItem;
import com.liuyuxiang.animeserver.dto.CommentItem;
import com.liuyuxiang.animeserver.dto.EpisodeItem;
import com.liuyuxiang.animeserver.service.AnimeService;
import com.liuyuxiang.animeserver.service.CommentService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 番剧列表接口
 *
 * <p>@RestController = @Controller + @ResponseBody，返回值由 Jackson 直接序列化成
 * JSON 写进响应体，不需要视图解析
 */
@RestController
@RequestMapping("/api")
public class AnimeController {

    private final AnimeService animeService;
    private final CommentService commentService;

    public AnimeController(AnimeService animeService, CommentService commentService) {
        this.animeService = animeService;
        this.commentService = commentService;
    }

    /**
     * 返回的是裸数组，没有 {code, data, msg} 外壳
     *
     * <p>前端拿到响应直接 data.sort(...) 再 slice，一旦包了外壳就会报错
     *
     * <p>只返回上架的番剧。管理后台要看到全部走 /api/admin/anime
     */
    @GetMapping("/anime")
    public List<AnimeItem> list() {
        return animeService.listVisible();
    }

    /**
     * 搜索
     *
     * <p>两个参数都可以不传。`q` 是名称关键词，`tags` 是标签 id 列表，
     * 多个标签是**同时满足**的关系
     *
     * <p>标签用 `?tags=1&tags=2` 或 `?tags=1,2` 都行，Spring 两种都能绑到 List
     */
    @GetMapping("/search")
    public List<AnimeItem> search(@RequestParam(required = false) String q,
                                  @RequestParam(required = false) List<Long> tags) {
        return animeService.search(q, tags);
    }

    /**
     * 番剧详情
     *
     * <p>下架的番剧也能取到，因为用户可能从自己的追番或观看记录点进来。
     * 是不是上架看返回体里的 visible
     */
    @GetMapping("/anime/{id}")
    public AnimeDetail detail(@PathVariable Long id) {
        return animeService.detail(id);
    }

    @GetMapping("/anime/{id}/episodes")
    public List<EpisodeItem> episodes(@PathVariable Long id) {
        return animeService.episodes(id);
    }

    /**
     * 评论列表
     *
     * <p>**读评论不需要登录**，所以这个路径不在拦截器的名单里。
     * 发评论走 POST /api/comments，那条才需要登录
     */
    @GetMapping("/anime/{id}/comments")
    public List<CommentItem> comments(@PathVariable Long id) {
        return commentService.list(id);
    }
}
