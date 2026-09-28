package com.liuyuxiang.animeserver.service;

import com.liuyuxiang.animeserver.dto.SaveTagRequest;
import com.liuyuxiang.animeserver.dto.TagItem;
import com.liuyuxiang.animeserver.exception.ApiException;
import com.liuyuxiang.animeserver.mapper.TagMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 标签的增删改查
 *
 * <p>标签由管理员创建，普通用户没有编辑番剧的入口，自然也不该能造标签，
 * 否则标签表会被灌进一堆同义词
 */
@Service
public class AdminTagService {

    private static final int MAX_NAME_LENGTH = 50;

    private final TagMapper tagMapper;

    public AdminTagService(TagMapper tagMapper) {
        this.tagMapper = tagMapper;
    }

    /** 公开搜索页用：计数只算上架的番剧 */
    public List<TagItem> listVisible() {
        return tagMapper.selectAll();
    }

    /** 管理端用：计数含下架的番剧 */
    public List<TagItem> listAll() {
        return tagMapper.selectAllForAdmin();
    }

    public TagItem create(SaveTagRequest request) {
        String name = normalizeName(request.getName());

        TagItem tag = new TagItem();
        tag.setName(name);
        try {
            tagMapper.insert(tag);
        } catch (DuplicateKeyException ex) {
            // name 上有唯一键。不做先查再插的预检，并发下那次预检会双双通过
            throw new ApiException(HttpStatus.CONFLICT, "标签「" + name + "」已经存在");
        }

        return tagMapper.selectById(tag.getId());
    }

    public TagItem rename(Long tagId, SaveTagRequest request) {
        if (tagMapper.selectById(tagId) == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "标签不存在");
        }
        String name = normalizeName(request.getName());

        try {
            tagMapper.update(tagId, name);
        } catch (DuplicateKeyException ex) {
            throw new ApiException(HttpStatus.CONFLICT, "标签「" + name + "」已经存在");
        }
        return tagMapper.selectById(tagId);
    }

    /**
     * 删标签
     *
     * <p>anime_tag 里引用它的行会级联清掉，**番剧本身不受影响**，
     * 只是那些番不再有这个标签了。前端要提示清楚这一点，
     * 免得管理员以为会连番剧一起删
     */
    public void delete(Long tagId) {
        if (tagMapper.delete(tagId) == 0) {
            throw new ApiException(HttpStatus.NOT_FOUND, "标签不存在");
        }
    }

    private String normalizeName(String raw) {
        String name = raw == null ? "" : raw.trim();
        if (name.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "标签名不能为空");
        }
        if (name.length() > MAX_NAME_LENGTH) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "标签名不能超过 " + MAX_NAME_LENGTH + " 个字符");
        }
        return name;
    }
}
