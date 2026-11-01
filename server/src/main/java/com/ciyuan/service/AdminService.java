package com.ciyuan.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ciyuan.common.BizException;
import com.ciyuan.common.PageResult;
import com.ciyuan.entity.*;
import com.ciyuan.mapper.*;
import com.ciyuan.vo.Vos;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserMapper userMapper;
    private final VideoMapper videoMapper;
    private final CommentMapper commentMapper;
    private final DanmakuMapper danmakuMapper;
    private final CategoryMapper categoryMapper;
    private final UserService userService;
    private final VideoService videoService;

    public Vos.Stats stats() {
        Vos.Stats s = new Vos.Stats();
        s.setUsers(userMapper.selectCount(null));
        s.setVideos(videoMapper.selectCount(null));
        s.setComments(commentMapper.selectCount(null));
        s.setDanmaku(danmakuMapper.selectCount(null));
        s.setPendingVideos(videoMapper.selectCount(new LambdaQueryWrapper<Video>().eq(Video::getStatus, 0)));
        LocalDateTime today = LocalDate.now().atStartOfDay();
        s.setTodayVideos(videoMapper.selectCount(new LambdaQueryWrapper<Video>().ge(Video::getCreatedAt, today)));
        s.setTodayUsers(userMapper.selectCount(new LambdaQueryWrapper<User>().ge(User::getCreatedAt, today)));
        QueryWrapper<Video> qw = new QueryWrapper<>();
        qw.select("COALESCE(SUM(views),0) AS total");
        List<Object> objs = videoMapper.selectObjs(qw);
        long views = 0;
        if (objs != null && !objs.isEmpty() && objs.get(0) != null) {
            views = Long.parseLong(String.valueOf(objs.get(0)));
        }
        s.setViews(views);
        return s;
    }

    public PageResult<Vos.VideoVO> videos(String keyword, Integer status, int page, int size) {
        LambdaQueryWrapper<Video> w = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) w.like(Video::getTitle, keyword.trim());
        if (status != null) w.eq(Video::getStatus, status);
        w.orderByDesc(Video::getId);
        Page<Video> p = videoMapper.selectPage(new Page<>(page, size), w);
        return PageResult.of(videoService.toVOs(p.getRecords()), p.getTotal(), page, size);
    }

    public PageResult<Vos.UserVO> users(String keyword, int page, int size) {
        LambdaQueryWrapper<User> w = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            w.and(q -> q.like(User::getUsername, keyword.trim()).or().like(User::getNickname, keyword.trim()));
        }
        w.orderByDesc(User::getId);
        Page<User> p = userMapper.selectPage(new Page<>(page, size), w);
        List<Vos.UserVO> list = p.getRecords().stream().map(userService::basic).toList();
        return PageResult.of(list, p.getTotal(), page, size);
    }

    public void setUserStatus(Long id, int status) {
        userMapper.update(null, new LambdaUpdateWrapper<User>().eq(User::getId, id).set(User::getStatus, status));
    }

    public void setUserRole(Long id, String role) {
        userMapper.update(null, new LambdaUpdateWrapper<User>().eq(User::getId, id).set(User::getRole, role));
    }

    public List<Category> categories() {
        return categoryMapper.selectList(new LambdaQueryWrapper<Category>().orderByAsc(Category::getSort));
    }

    public Category saveCategory(Category c) {
        if (!StringUtils.hasText(c.getName())) throw new BizException("分类名不能为空");
        if (c.getId() == null) {
            if (!StringUtils.hasText(c.getSlug())) c.setSlug("c" + System.currentTimeMillis() % 100000);
            if (c.getSort() == null) c.setSort(99);
            categoryMapper.insert(c);
        } else {
            categoryMapper.updateById(c);
        }
        return c;
    }

    public void deleteCategory(Long id) {
        Long used = videoMapper.selectCount(new LambdaQueryWrapper<Video>().eq(Video::getCategoryId, id));
        if (used != null && used > 0) throw new BizException("该分区下还有 " + used + " 个视频，无法删除");
        categoryMapper.deleteById(id);
    }
}
