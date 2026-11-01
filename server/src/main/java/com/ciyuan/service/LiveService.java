package com.ciyuan.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ciyuan.common.BizException;
import com.ciyuan.common.PageResult;
import com.ciyuan.common.UserContext;
import com.ciyuan.dto.LiveRequests;
import com.ciyuan.entity.*;
import com.ciyuan.mapper.*;
import com.ciyuan.vo.LiveVos;
import com.ciyuan.vo.Vos;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LiveService {

    private final LiveRoomMapper roomMapper;
    private final LiveMessageMapper messageMapper;
    private final LiveViewerMapper viewerMapper;
    private final LiveLikeMapper likeMapper;
    private final UserMapper userMapper;
    private final CategoryMapper categoryMapper;
    private final UserService userService;

    @Value("${ciyuan.live.push-host:121.43.245.66}")
    private String pushHost;

    @Value("${ciyuan.live.rtmp-port:1935}")
    private int rtmpPort;

    @Value("${ciyuan.live.hls-prefix:/livehls}")
    private String hlsPrefix;

    private static final int ONLINE_WINDOW_SEC = 60;

    // ---------------------------------------------------------------- 查询

    public PageResult<LiveVos.LiveRoomVO> page(int page, int size, Integer status, Long categoryId) {
        LambdaQueryWrapper<LiveRoom> w = new LambdaQueryWrapper<>();
        if (status != null) w.eq(LiveRoom::getStatus, status);
        if (categoryId != null && categoryId > 0) w.eq(LiveRoom::getCategoryId, categoryId);
        w.orderByDesc(LiveRoom::getStatus).orderByDesc(LiveRoom::getViewers).orderByDesc(LiveRoom::getId);
        Page<LiveRoom> p = roomMapper.selectPage(new Page<>(page, size), w);
        return PageResult.of(toVOs(p.getRecords()), p.getTotal(), page, size);
    }

    public LiveVos.LiveRoomVO detail(Long id) {
        return toVOs(List.of(requireRoom(id))).get(0);
    }

    public LiveVos.LiveRoomVO my() {
        Long uid = UserContext.requireId();
        LiveRoom room = roomMapper.selectOne(new LambdaQueryWrapper<LiveRoom>().eq(LiveRoom::getUserId, uid));
        return room == null ? null : toVOs(List.of(room)).get(0);
    }

    // ---------------------------------------------------------------- 房间管理

    public LiveVos.LiveRoomVO create(LiveRequests.CreateRoom req) {
        Long uid = UserContext.requireId();
        LiveRoom room = roomMapper.selectOne(new LambdaQueryWrapper<LiveRoom>().eq(LiveRoom::getUserId, uid));
        if (room == null) {
            room = new LiveRoom();
            room.setUserId(uid);
            room.setTitle(req.getTitle().trim());
            room.setCover(req.getCover());
            room.setCategoryId(req.getCategoryId() == null ? 0L : req.getCategoryId());
            room.setStreamKey(UUID.randomUUID().toString().replace("-", "").substring(0, 16));
            room.setStatus(0);
            room.setViewers(0L);
            room.setPeakViewers(0L);
            room.setTotalViewers(0L);
            room.setLikeCount(0L);
            roomMapper.insert(room);
        } else {
            room.setTitle(req.getTitle().trim());
            room.setCover(req.getCover());
            room.setCategoryId(req.getCategoryId() == null ? 0L : req.getCategoryId());
            roomMapper.updateById(room);
        }
        return toVOs(List.of(roomMapper.selectById(room.getId()))).get(0);
    }

    public LiveVos.LiveRoomVO update(Long id, LiveRequests.CreateRoom req) {
        LiveRoom room = requireOwned(id);
        room.setTitle(req.getTitle().trim());
        room.setCover(req.getCover());
        room.setCategoryId(req.getCategoryId() == null ? 0L : req.getCategoryId());
        roomMapper.updateById(room);
        return toVOs(List.of(roomMapper.selectById(id))).get(0);
    }

    public LiveVos.LiveRoomVO start(Long id) {
        LiveRoom room = requireOwned(id);
        room.setStatus(1);
        room.setStartedAt(LocalDateTime.now());
        room.setEndedAt(null);
        room.setViewers(0L);
        room.setPeakViewers(0L);
        roomMapper.updateById(room);
        return toVOs(List.of(roomMapper.selectById(id))).get(0);
    }

    public LiveVos.LiveRoomVO stop(Long id) {
        LiveRoom room = requireOwned(id);
        room.setStatus(2);
        room.setEndedAt(LocalDateTime.now());
        room.setViewers(0L);
        roomMapper.updateById(room);
        return toVOs(List.of(roomMapper.selectById(id))).get(0);
    }

    public void delete(Long id) {
        requireOwned(id);
        roomMapper.deleteById(id);
        messageMapper.delete(new LambdaQueryWrapper<LiveMessage>().eq(LiveMessage::getRoomId, id));
        viewerMapper.delete(new LambdaQueryWrapper<LiveViewer>().eq(LiveViewer::getRoomId, id));
        likeMapper.delete(new LambdaQueryWrapper<LiveLike>().eq(LiveLike::getRoomId, id));
    }

    public Vos.ToggleResult like(Long id) {
        Long uid = UserContext.requireId();
        requireRoom(id);
        LiveLike exist = likeMapper.selectOne(new LambdaQueryWrapper<LiveLike>()
                .eq(LiveLike::getRoomId, id).eq(LiveLike::getUserId, uid));
        boolean active;
        if (exist == null) {
            LiveLike lk = new LiveLike();
            lk.setRoomId(id);
            lk.setUserId(uid);
            likeMapper.insert(lk);
            roomMapper.update(null, new LambdaUpdateWrapper<LiveRoom>()
                    .eq(LiveRoom::getId, id).setSql("like_count = like_count + 1"));
            active = true;
        } else {
            likeMapper.deleteById(exist.getId());
            roomMapper.update(null, new LambdaUpdateWrapper<LiveRoom>()
                    .eq(LiveRoom::getId, id).setSql("like_count = GREATEST(like_count - 1, 0)"));
            active = false;
        }
        Long count = roomMapper.selectById(id).getLikeCount();
        Vos.ToggleResult r = new Vos.ToggleResult();
        r.setActive(active);
        r.setCount(count);
        r.setMsg(active ? "点赞成功" : "已取消");
        return r;
    }

    // ---------------------------------------------------------------- 在线

    public Long heartbeat(Long id) {
        Long uid = UserContext.requireId();
        requireRoom(id);
        LocalDateTime now = LocalDateTime.now();
        LiveViewer v = viewerMapper.selectOne(new LambdaQueryWrapper<LiveViewer>()
                .eq(LiveViewer::getRoomId, id).eq(LiveViewer::getUserId, uid));
        if (v == null) {
            v = new LiveViewer();
            v.setRoomId(id);
            v.setUserId(uid);
            v.setLastSeen(now);
            viewerMapper.insert(v);
            roomMapper.update(null, new LambdaUpdateWrapper<LiveRoom>()
                    .eq(LiveRoom::getId, id).setSql("total_viewers = total_viewers + 1"));
        } else {
            v.setLastSeen(now);
            viewerMapper.updateById(v);
        }
        long online = onlineCount(id);
        roomMapper.update(null, new LambdaUpdateWrapper<LiveRoom>()
                .eq(LiveRoom::getId, id)
                .set(LiveRoom::getViewers, online)
                .setSql("peak_viewers = GREATEST(peak_viewers, " + online + ")"));
        return online;
    }

    public long onlineCount(Long id) {
        LocalDateTime cutoff = LocalDateTime.now().minusSeconds(ONLINE_WINDOW_SEC);
        return viewerMapper.selectCount(new LambdaQueryWrapper<LiveViewer>()
                .eq(LiveViewer::getRoomId, id).ge(LiveViewer::getLastSeen, cutoff));
    }

    // ---------------------------------------------------------------- 消息

    public List<LiveVos.LiveMessageVO> messages(Long roomId, Long afterId, int limit) {
        requireRoom(roomId);
        int lim = Math.max(1, Math.min(limit, 200));
        LambdaQueryWrapper<LiveMessage> w = new LambdaQueryWrapper<LiveMessage>()
                .eq(LiveMessage::getRoomId, roomId);
        if (afterId != null && afterId > 0) w.gt(LiveMessage::getId, afterId);
        w.orderByAsc(LiveMessage::getId).last("limit " + lim);
        return toMessageVOs(messageMapper.selectList(w));
    }

    public LiveVos.LiveMessageVO send(Long roomId, LiveRequests.MessageReq req) {
        Long uid = UserContext.requireId();
        requireRoom(roomId);
        LiveMessage m = new LiveMessage();
        m.setRoomId(roomId);
        m.setUserId(uid);
        m.setContent(req.getContent().trim());
        m.setType(req.getType() == null ? 0 : req.getType());
        m.setColor(StringUtils.hasText(req.getColor()) ? req.getColor() : "#ffffff");
        messageMapper.insert(m);
        return toMessageVOs(List.of(messageMapper.selectById(m.getId()))).get(0);
    }

    // ---------------------------------------------------------------- 内部

    private LiveRoom requireRoom(Long id) {
        LiveRoom room = roomMapper.selectById(id);
        if (room == null) throw BizException.notFound("直播间");
        return room;
    }

    private LiveRoom requireOwned(Long id) {
        LiveRoom room = requireRoom(id);
        Long uid = UserContext.requireId();
        if (!Objects.equals(room.getUserId(), uid) && !UserContext.isAdmin()) {
            throw BizException.forbidden();
        }
        return room;
    }

    private List<LiveVos.LiveRoomVO> toVOs(List<LiveRoom> rooms) {
        if (rooms == null || rooms.isEmpty()) return List.of();
        Set<Long> uids = rooms.stream().map(LiveRoom::getUserId).collect(Collectors.toSet());
        Map<Long, User> users = userMapper.selectBatchIds(uids).stream()
                .collect(Collectors.toMap(User::getId, x -> x));
        Set<Long> cids = rooms.stream().map(LiveRoom::getCategoryId)
                .filter(Objects::nonNull).filter(x -> x > 0).collect(Collectors.toSet());
        Map<Long, String> catNames = cids.isEmpty() ? Map.of() :
                categoryMapper.selectBatchIds(cids).stream()
                        .collect(Collectors.toMap(Category::getId, Category::getName));
        Long me = UserContext.id();
        Set<Long> likedIds = me == null ? Set.of() :
                likeMapper.selectList(new LambdaQueryWrapper<LiveLike>()
                                .eq(LiveLike::getUserId, me)
                                .in(LiveLike::getRoomId, rooms.stream().map(LiveRoom::getId).toList()))
                        .stream().map(LiveLike::getRoomId).collect(Collectors.toSet());

        List<LiveVos.LiveRoomVO> out = new ArrayList<>();
        for (LiveRoom r : rooms) {
            LiveVos.LiveRoomVO vo = new LiveVos.LiveRoomVO();
            vo.setId(r.getId());
            vo.setUserId(r.getUserId());
            vo.setTitle(r.getTitle());
            vo.setCover(r.getCover());
            vo.setCategoryId(r.getCategoryId());
            vo.setCategoryName(catNames.get(r.getCategoryId()));
            vo.setStatus(r.getStatus());
            vo.setViewers(r.getViewers() == null ? 0 : r.getViewers());
            vo.setPeakViewers(r.getPeakViewers() == null ? 0 : r.getPeakViewers());
            vo.setTotalViewers(r.getTotalViewers() == null ? 0 : r.getTotalViewers());
            vo.setLikeCount(r.getLikeCount() == null ? 0 : r.getLikeCount());
            vo.setStartedAt(r.getStartedAt());
            vo.setEndedAt(r.getEndedAt());
            vo.setCreatedAt(r.getCreatedAt());
            User u = users.get(r.getUserId());
            if (u != null) vo.setAuthor(userService.basic(u));
            boolean mine = me != null && Objects.equals(r.getUserId(), me);
            vo.setMine(mine);
            vo.setLiked(likedIds.contains(r.getId()));
            vo.setPlayUrl(hlsPrefix + "/" + r.getStreamKey() + ".m3u8");
            if (mine) {
                vo.setStreamKey(r.getStreamKey());
                vo.setPushUrl("rtmp://" + pushHost + ":" + rtmpPort + "/live/" + r.getStreamKey());
            } else {
                vo.setStreamKey("****");
            }
            out.add(vo);
        }
        return out;
    }

    private List<LiveVos.LiveMessageVO> toMessageVOs(List<LiveMessage> list) {
        if (list == null || list.isEmpty()) return List.of();
        Set<Long> uids = list.stream().map(LiveMessage::getUserId).collect(Collectors.toSet());
        Map<Long, User> users = userMapper.selectBatchIds(uids).stream()
                .collect(Collectors.toMap(User::getId, x -> x));
        List<LiveVos.LiveMessageVO> out = new ArrayList<>();
        for (LiveMessage m : list) {
            LiveVos.LiveMessageVO vo = new LiveVos.LiveMessageVO();
            vo.setId(m.getId());
            vo.setRoomId(m.getRoomId());
            vo.setUserId(m.getUserId());
            vo.setContent(m.getContent());
            vo.setType(m.getType());
            vo.setColor(m.getColor());
            vo.setCreatedAt(m.getCreatedAt());
            User u = users.get(m.getUserId());
            if (u != null) vo.setUser(userService.basic(u));
            out.add(vo);
        }
        return out;
    }
}
