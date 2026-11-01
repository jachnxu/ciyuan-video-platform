package com.ciyuan.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ciyuan.common.PageResult;
import com.ciyuan.entity.Notification;
import com.ciyuan.entity.User;
import com.ciyuan.entity.Video;
import com.ciyuan.mapper.NotificationMapper;
import com.ciyuan.mapper.UserMapper;
import com.ciyuan.mapper.VideoMapper;
import com.ciyuan.vo.Vos;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationMapper notificationMapper;
    private final UserMapper userMapper;
    private final VideoMapper videoMapper;
    private final UserService userService;

    public void push(Long userId, Long fromUserId, String type, Long videoId, String content) {
        if (userId == null || userId.equals(fromUserId)) return;
        Notification n = new Notification();
        n.setUserId(userId);
        n.setFromUserId(fromUserId);
        n.setType(type);
        n.setVideoId(videoId);
        n.setContent(content);
        n.setIsRead(0);
        notificationMapper.insert(n);
    }

    public PageResult<Vos.NotifVO> list(int page, int size) {
        Long me = com.ciyuan.common.UserContext.requireId();
        Page<Notification> p = notificationMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<Notification>().eq(Notification::getUserId, me).orderByDesc(Notification::getId));
        List<Notification> records = p.getRecords();
        Map<Long, User> users = Map.of();
        Map<Long, Video> videos = Map.of();
        if (!records.isEmpty()) {
            var uids = records.stream().map(Notification::getFromUserId).filter(java.util.Objects::nonNull).distinct().toList();
            if (!uids.isEmpty()) {
                users = userMapper.selectBatchIds(uids).stream().collect(Collectors.toMap(User::getId, Function.identity()));
            }
            var vids = records.stream().map(Notification::getVideoId).filter(java.util.Objects::nonNull).distinct().toList();
            if (!vids.isEmpty()) {
                videos = videoMapper.selectBatchIds(vids).stream().collect(Collectors.toMap(Video::getId, Function.identity()));
            }
        }
        final Map<Long, User> um = users;
        final Map<Long, Video> vm = videos;
        List<Vos.NotifVO> list = records.stream().map(n -> {
            Vos.NotifVO vo = new Vos.NotifVO();
            vo.setId(n.getId());
            vo.setType(n.getType());
            vo.setContent(n.getContent());
            vo.setIsRead(n.getIsRead());
            vo.setCreatedAt(n.getCreatedAt());
            vo.setVideoId(n.getVideoId());
            vo.setFromUser(userService.basic(um.get(n.getFromUserId())));
            Video v = vm.get(n.getVideoId());
            if (v != null) {
                vo.setVideoTitle(v.getTitle());
                vo.setVideoCover(v.getCover());
            }
            return vo;
        }).toList();
        return PageResult.of(list, p.getTotal(), page, size);
    }

    public long unread() {
        Long me = com.ciyuan.common.UserContext.requireId();
        return notificationMapper.selectCount(new LambdaQueryWrapper<Notification>()
                .eq(Notification::getUserId, me).eq(Notification::getIsRead, 0));
    }

    public void readAll() {
        Long me = com.ciyuan.common.UserContext.requireId();
        notificationMapper.update(null, new LambdaUpdateWrapper<Notification>()
                .eq(Notification::getUserId, me).eq(Notification::getIsRead, 0).set(Notification::getIsRead, 1));
    }
}
