package com.ciyuan.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ciyuan.common.BizException;
import com.ciyuan.common.PageResult;
import com.ciyuan.common.UserContext;
import com.ciyuan.dto.Requests;
import com.ciyuan.entity.*;
import com.ciyuan.mapper.*;
import com.ciyuan.vo.Vos;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentMapper commentMapper;
    private final CommentLikeMapper commentLikeMapper;
    private final UserMapper userMapper;
    private final VideoMapper videoMapper;
    private final UserService userService;
    private final NotificationService notificationService;

    public PageResult<Vos.CommentVO> list(Long videoId, int page, int size, String sort) {
        LambdaQueryWrapper<Comment> w = new LambdaQueryWrapper<Comment>()
                .eq(Comment::getVideoId, videoId).eq(Comment::getRootId, 0L);
        if ("hot".equals(sort)) {
            w.orderByDesc(Comment::getLikeCount).orderByDesc(Comment::getId);
        } else {
            w.orderByDesc(Comment::getId);
        }
        Page<Comment> p = commentMapper.selectPage(new Page<>(page, size), w);
        List<Comment> roots = p.getRecords();
        if (roots.isEmpty()) return PageResult.of(List.of(), p.getTotal(), page, size);

        List<Long> rootIds = roots.stream().map(Comment::getId).toList();
        List<Comment> children = commentMapper.selectList(new LambdaQueryWrapper<Comment>()
                .in(Comment::getRootId, rootIds).orderByAsc(Comment::getId));

        Map<Long, List<Comment>> grouped = children.stream().collect(Collectors.groupingBy(Comment::getRootId));
        Map<Long, Long> childCount = children.stream().collect(Collectors.groupingBy(Comment::getRootId, Collectors.counting()));

        Set<Long> uids = new HashSet<>();
        roots.forEach(c -> uids.add(c.getUserId()));
        children.forEach(c -> {
            uids.add(c.getUserId());
            if (c.getReplyUserId() != null) uids.add(c.getReplyUserId());
        });
        Map<Long, User> users = userMapper.selectBatchIds(uids).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        Long me = UserContext.id();
        Set<Long> likedIds = new HashSet<>();
        List<Long> allIds = new ArrayList<>(rootIds);
        children.forEach(c -> allIds.add(c.getId()));
        if (me != null && !allIds.isEmpty()) {
            commentLikeMapper.selectList(new LambdaQueryWrapper<CommentLike>()
                            .eq(CommentLike::getUserId, me).in(CommentLike::getCommentId, allIds))
                    .forEach(x -> likedIds.add(x.getCommentId()));
        }

        List<Vos.CommentVO> list = new ArrayList<>();
        for (Comment root : roots) {
            Vos.CommentVO vo = toVO(root, users, likedIds);
            List<Comment> cs = grouped.getOrDefault(root.getId(), List.of());
            vo.setChildCount(cs.size());
            vo.setChildren(cs.stream().limit(3).map(c -> toVO(c, users, likedIds)).toList());
            list.add(vo);
        }
        return PageResult.of(list, p.getTotal(), page, size);
    }

    public List<Vos.CommentVO> children(Long rootId) {
        List<Comment> cs = commentMapper.selectList(new LambdaQueryWrapper<Comment>()
                .eq(Comment::getRootId, rootId).orderByAsc(Comment::getId));
        if (cs.isEmpty()) return List.of();
        Set<Long> uids = new HashSet<>();
        cs.forEach(c -> {
            uids.add(c.getUserId());
            if (c.getReplyUserId() != null) uids.add(c.getReplyUserId());
        });
        Map<Long, User> users = userMapper.selectBatchIds(uids).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
        Long me = UserContext.id();
        Set<Long> likedIds = new HashSet<>();
        if (me != null) {
            commentLikeMapper.selectList(new LambdaQueryWrapper<CommentLike>()
                            .eq(CommentLike::getUserId, me).in(CommentLike::getCommentId, cs.stream().map(Comment::getId).toList()))
                    .forEach(x -> likedIds.add(x.getCommentId()));
        }
        return cs.stream().map(c -> toVO(c, users, likedIds)).toList();
    }

    public Vos.CommentVO add(Requests.CommentReq req) {
        Long me = UserContext.requireId();
        if (req.getVideoId() == null) throw new BizException("缺少视频 id");
        Video v = videoMapper.selectById(req.getVideoId());
        if (v == null) throw BizException.notFound("视频");
        Comment c = new Comment();
        c.setVideoId(req.getVideoId());
        c.setUserId(me);
        c.setContent(req.getContent().trim());
        c.setLikeCount(0L);
        long rootId = 0L;
        long parentId = 0L;
        if (req.getParentId() != null && req.getParentId() > 0) {
            Comment parent = commentMapper.selectById(req.getParentId());
            if (parent == null) throw BizException.notFound("评论");
            parentId = parent.getId();
            rootId = parent.getRootId() != null && parent.getRootId() > 0 ? parent.getRootId() : parent.getId();
            c.setReplyUserId(parent.getUserId());
            notificationService.push(parent.getUserId(), me, "comment", v.getId(), "回复了你的评论：" + brief(c.getContent()));
        } else {
            notificationService.push(v.getUserId(), me, "comment", v.getId(), "评论了你的视频：" + brief(c.getContent()));
        }
        c.setRootId(rootId);
        c.setParentId(parentId);
        commentMapper.insert(c);
        if (c.getCreatedAt() == null) c.setCreatedAt(java.time.LocalDateTime.now());
        videoMapper.update(null, new LambdaUpdateWrapper<Video>().eq(Video::getId, v.getId())
                .setSql("comment_count = comment_count + 1"));
        Map<Long, User> users = userMapper.selectBatchIds(Set.of(me)).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
        return toVO(c, users, Set.of());
    }

    public void delete(Long id) {
        Long me = UserContext.requireId();
        Comment c = commentMapper.selectById(id);
        if (c == null) throw BizException.notFound("评论");
        Video v = videoMapper.selectById(c.getVideoId());
        boolean owner = Objects.equals(c.getUserId(), me)
                || (v != null && Objects.equals(v.getUserId(), me))
                || UserContext.isAdmin();
        if (!owner) throw BizException.forbidden();
        long removed = 1;
        if (c.getRootId() != null && c.getRootId() == 0) {
            List<Comment> children = commentMapper.selectList(new LambdaQueryWrapper<Comment>()
                    .eq(Comment::getRootId, c.getId()));
            children.forEach(x -> commentMapper.deleteById(x.getId()));
            removed += children.size();
        }
        commentMapper.deleteById(id);
        videoMapper.update(null, new LambdaUpdateWrapper<Video>().eq(Video::getId, c.getVideoId())
                .setSql("comment_count = GREATEST(comment_count - " + removed + ", 0)"));
    }

    public Vos.ToggleResult like(Long id) {
        Long me = UserContext.requireId();
        Comment c = commentMapper.selectById(id);
        if (c == null) throw BizException.notFound("评论");
        CommentLike exist = commentLikeMapper.selectOne(new LambdaQueryWrapper<CommentLike>()
                .eq(CommentLike::getCommentId, id).eq(CommentLike::getUserId, me));
        Vos.ToggleResult r = new Vos.ToggleResult();
        if (exist != null) {
            commentLikeMapper.deleteById(exist.getId());
            commentMapper.update(null, new LambdaUpdateWrapper<Comment>().eq(Comment::getId, id)
                    .setSql("like_count = GREATEST(like_count - 1, 0)"));
            r.setActive(false);
        } else {
            CommentLike cl = new CommentLike();
            cl.setCommentId(id);
            cl.setUserId(me);
            commentLikeMapper.insert(cl);
            commentMapper.update(null, new LambdaUpdateWrapper<Comment>().eq(Comment::getId, id)
                    .setSql("like_count = like_count + 1"));
            r.setActive(true);
        }
        Long cnt = commentLikeMapper.selectCount(new LambdaQueryWrapper<CommentLike>().eq(CommentLike::getCommentId, id));
        r.setCount(cnt == null ? 0 : cnt);
        return r;
    }

    private Vos.CommentVO toVO(Comment c, Map<Long, User> users, Set<Long> likedIds) {
        Vos.CommentVO vo = new Vos.CommentVO();
        vo.setId(c.getId());
        vo.setContent(c.getContent());
        vo.setCreatedAt(c.getCreatedAt());
        vo.setLikeCount(c.getLikeCount() == null ? 0 : c.getLikeCount());
        vo.setUser(userService.basic(users.get(c.getUserId())));
        if (c.getReplyUserId() != null && c.getReplyUserId() > 0) {
            vo.setReplyUser(userService.basic(users.get(c.getReplyUserId())));
        }
        vo.setLiked(likedIds.contains(c.getId()));
        return vo;
    }

    private String brief(String s) {
        return s.length() > 30 ? s.substring(0, 30) + "..." : s;
    }
}
