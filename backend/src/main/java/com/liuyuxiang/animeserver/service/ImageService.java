package com.liuyuxiang.animeserver.service;

import com.liuyuxiang.animeserver.exception.ApiException;
import com.liuyuxiang.animeserver.mapper.AnimeMapper;
import com.liuyuxiang.animeserver.mapper.BannerMapper;
import com.liuyuxiang.animeserver.mapper.UserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * 上传图片的落盘与清理
 *
 * <p>存下来的地址一律是 /uploads/xxx.jpg 这种站内相对形式，
 * 前端用 resolveImageUrl 补上后端的源。库里不存完整 URL，
 * 否则后端一换端口所有历史数据里的地址就全废了
 */
@Service
public class ImageService {

    private static final Logger log = LoggerFactory.getLogger(ImageService.class);

    /** 上传图片的路径前缀。预置图片是 /covers/ 和 /banners/，不走这里 */
    public static final String UPLOAD_URL_PREFIX = "/uploads/";

    /**
     * 只放行这几种。用白名单不用黑名单，黑名单漏一个就完了，
     * 而且 .jsp .svg 这类能被当成脚本执行的一旦漏进去就是大问题
     */
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");

    private final Path uploadDir;
    private final AnimeMapper animeMapper;
    private final BannerMapper bannerMapper;
    private final UserMapper userMapper;

    public ImageService(@Value("${app.upload.dir}") String uploadDir,
                        AnimeMapper animeMapper, BannerMapper bannerMapper, UserMapper userMapper) {
        this.uploadDir = Paths.get(uploadDir).toAbsolutePath().normalize();
        this.animeMapper = animeMapper;
        this.bannerMapper = bannerMapper;
        this.userMapper = userMapper;
    }

    /**
     * 存一张上传的图，返回它的站内相对地址
     *
     * <p>文件名**重新生成**，绝不沿用客户端传来的名字。用户传
     * ../../application.yml 这种名字就是目录穿越，传个已存在的名字就会覆盖别人的图
     */
    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "请选择要上传的图片");
        }

        String extension = extensionOf(file.getOriginalFilename());
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "只支持 jpg png webp 格式的图片");
        }

        String filename = UUID.randomUUID() + "." + extension;
        try {
            Files.createDirectories(uploadDir);
            file.transferTo(uploadDir.resolve(filename));
        } catch (IOException ex) {
            log.error("保存上传图片失败", ex);
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "图片保存失败");
        }

        return UPLOAD_URL_PREFIX + filename;
    }

    /**
     * 删掉一个不再被任何记录引用的上传图片
     *
     * <p>三道闸，任何一道不过就什么都不做
     *
     * <p>一是**只认 uploads 下的路径**。frontend/public 里的封面和轮播图是仓库里的源文件，
     * 可能被多条记录共用，删掉会连带毁掉别处的数据，而且不可恢复
     *
     * <p>二是**删之前查引用**。同一个文件可能被 anime.cover_image、
     * banner_poster.image_url 或 app_user.avatar 引用，还有记录在用就不能删
     *
     * <p>三是**调用时机必须在数据库事务提交之后**。反过来做的话，事务一旦回滚就会出现
     * 记录还在图片没了的裂图，且无法恢复。按现在这个顺序，最坏情况是删文件失败留下
     * 一个孤儿文件，不影响任何功能
     */
    public void deleteIfOrphaned(String url) {
        String filename = filenameOf(url);
        if (filename == null) {
            return;
        }
        if (animeMapper.countByCoverImage(url) > 0
                || bannerMapper.countByImageUrl(url) > 0
                || userMapper.countByAvatar(url) > 0) {
            return;
        }

        Path file = uploadDir.resolve(filename);
        try {
            if (Files.deleteIfExists(file)) {
                log.info("已删除无引用的图片 {}", file);
            }
        } catch (IOException ex) {
            // 删文件失败不回滚数据库，也不抛给调用方。留下一个孤儿文件比让删番剧失败要好
            log.warn("删除图片失败 {}", file, ex);
        }
    }

    /** 取小写扩展名，没有就返回空串 */
    private String extensionOf(String originalFilename) {
        if (originalFilename == null) {
            return "";
        }
        int dot = originalFilename.lastIndexOf('.');
        if (dot < 0 || dot == originalFilename.length() - 1) {
            return "";
        }
        return originalFilename.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    /**
     * 从路径里取出文件名，取不到就返回 null
     *
     * <p>取到之后还要再挡一次目录穿越。库里的值是本系统写进去的，正常不会有 ..，
     * 但不能假设——一旦有，删的就不是 uploads 下的文件了
     */
    private String filenameOf(String url) {
        if (url == null) {
            return null;
        }
        int index = url.indexOf(UPLOAD_URL_PREFIX);
        if (index < 0) {
            return null;
        }
        String filename = url.substring(index + UPLOAD_URL_PREFIX.length());
        if (filename.isEmpty() || filename.contains("/") || filename.contains("\\") || filename.contains("..")) {
            log.warn("图片路径不合法，已跳过 {}", url);
            return null;
        }
        return filename;
    }
}
