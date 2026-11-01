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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class VideoService {

    private final VideoMapper videoMapper;
    private final UserMapper userMapper;
    private final CategoryMapper categoryMapper;
    private final VideoLikeMapper likeMapper;
    private final VideoCoinMapper coinMapper;
    private final VideoFavMapper favMapper;
    private final HistoryMapper historyMapper;
    private final MediaService mediaService;
    private final UserService userService;
    private final NotificationService notificationService;

    @Qualifier("mediaExecutor")
    private final Executor mediaExecutor;

    private static final Set<String> ALLOWED_EXT = Set.of("mp4", "m4v", "webm", "mov", "mkv", "avi", "flv", "ts");

    // ---------------------------------------------------------------- 查询

    public PageResult<Vos.VideoVO> page(int page, int size, Long categoryId, String keyword,
                                        String sort, Long userId, Integer status) {
        LambdaQueryWrapper<Video> w = new LambdaQueryWrapper<>();
        if (status != null) {
            w.eq(Video::getStatus, status);
        } else {
            w.eq(Video::getStatus, 1);
        }
        if (categoryId != null && categoryId > 0) w.eq(Video::getCategoryId, categoryId);
        if (userId != null) w.eq(Video::getUserId, userId);
        if (StringUtils.hasText(keyword)) {
            String k = keyword.trim();
            w.and(q -> q.like(Video::getTitle, k).or().like(Video::getTags, k).or().like(Video::getDescription, k));
        }
        switch (sort == null ? "new" : sort) {
            case "hot" -> w.orderByDesc(Video::getViews).orderByDesc(Video::getId);
            case "danmaku" -> w.orderByDesc(Video::getDanmakuCount).orderByDesc(Video::getId);
            case "coin" -> w.orderByDesc(Video::getCoinCount).orderByDesc(Video::getId);
            case "like" -> w.orderByDesc(Video::getLikeCount).orderByDesc(Video::getId);
            default -> w.orderByDesc(Video::getCreatedAt).orderByDesc(Video::getId);
        }
        Page<Video> p = videoMapper.selectPage(new Page<>(page, size), w);
        return PageResult.of(toVOs(p.getRecords()), p.getTotal(), page, size);
    }

    public Vos.VideoVO detail(Long id) {
        Video v = videoMapper.selectById(id);
        if (v == null) throw BizException.notFound("视频");
        if (v.getStatus() != null && v.getStatus() == 2 && !UserContext.isAdmin()
                && !Objects.equals(v.getUserId(), UserContext.id())) {
            throw new BizException(404, "视频已下架");
        }
        videoMapper.update(null, new LambdaUpdateWrapper<Video>().eq(Video::getId, id).setSql("views = views + 1"));
        v.setViews((v.getViews() == null ? 0 : v.getViews()) + 1);
        Vos.VideoVO vo = toVOs(List.of(v)).get(0);
        Long me = UserContext.id();
        if (me != null) recordHistory(id, null);
        return vo;
    }

    public List<Vos.VideoVO> related(Long id, int limit) {
        Video v = videoMapper.selectById(id);
        if (v == null) return List.of();
        LambdaQueryWrapper<Video> w = new LambdaQueryWrapper<Video>()
                .eq(Video::getStatus, 1).ne(Video::getId, id);
        if (v.getCategoryId() != null) {
            w.eq(Video::getCategoryId, v.getCategoryId());
        }
        w.orderByDesc(Video::getViews).last("limit " + Math.min(limit, 20));
        List<Video> list = videoMapper.selectList(w);
        if (list.size() < limit) {
            List<Video> extra = videoMapper.selectList(new LambdaQueryWrapper<Video>()
                    .eq(Video::getStatus, 1).ne(Video::getId, id)
                    .orderByDesc(Video::getCreatedAt).last("limit " + Math.min(limit, 20)));
            Map<Long, Video> merged = new LinkedHashMap<>();
            list.forEach(x -> merged.put(x.getId(), x));
            extra.forEach(x -> merged.putIfAbsent(x.getId(), x));
            list = new ArrayList<>(merged.values());
        }
        return toVOs(list.stream().limit(limit).toList());
    }

    // ---------------------------------------------------------------- 投稿

    public Vos.VideoVO publish(MultipartFile file, MultipartFile cover, Requests.Publish req) {
        Long me = UserContext.requireId();
        if (file == null || file.isEmpty()) throw new BizException("请选择视频文件");
        String ext = mediaService.extOf(file.getOriginalFilename());
        if (!ALLOWED_EXT.contains(ext)) throw new BizException("不支持的视频格式：" + ext);

        String url = mediaService.save(file, "video");
        String coverUrl = null;
        if (cover != null && !cover.isEmpty()) {
            coverUrl = mediaService.save(cover, "cover");
        }
        String[] info = mediaService.probe(url);
        int duration = 0, width = 0, height = 0;
        boolean friendly = false;
        if (info != null) {
            duration = parseInt(info[0]);
            width = parseInt(info[1]);
            height = parseInt(info[2]);
            friendly = mediaService.isBrowserFriendly(info[3], info[4]);
        }

        Video v = new Video();
        v.setUserId(me);
        v.setCategoryId(req.getCategoryId() == null ? 1L : req.getCategoryId());
        v.setTitle(req.getTitle().trim());
        v.setDescription(req.getDescription() == null ? "" : req.getDescription());
        v.setCover(coverUrl);
        v.setVideoUrl(url);
        v.setDuration(duration);
        v.setWidth(width);
        v.setHeight(height);
        v.setStatus(friendly ? 1 : 0);
        v.setViews(0L);
        v.setDanmakuCount(0L);
        v.setLikeCount(0L);
        v.setCoinCount(0L);
        v.setFavCount(0L);
        v.setCommentCount(0L);
        v.setTags(req.getTags() == null ? "" : req.getTags().trim());
        videoMapper.insert(v);

        final Long vid = v.getId();
        final boolean needCover = coverUrl == null;
        final boolean needTranscode = !friendly;
        mediaExecutor.execute(() -> postProcess(vid, url, needCover, needTranscode));

        return toVOs(List.of(v)).get(0);
    }

    private void postProcess(Long vid, String url, boolean needCover, boolean needTranscode) {
        try {
            Video v = videoMapper.selectById(vid);
            if (v == null) return;
            String playUrl = url;
            if (needTranscode) {
                log.info("start transcode video {}", vid);
                String converted = mediaService.transcodeToMp4(url);
                if (converted != null) {
                    playUrl = converted;
                    String[] info = mediaService.probe(converted);
                    LambdaUpdateWrapper<Video> u = new LambdaUpdateWrapper<Video>().eq(Video::getId, vid)
                            .set(Video::getVideoUrl, converted)
                            .set(Video::getStatus, 1);
                    if (info != null) {
                        u.set(Video::getDuration, parseInt(info[0]));
                        u.set(Video::getWidth, parseInt(info[1]));
                        u.set(Video::getHeight, parseInt(info[2]));
                    }
                    videoMapper.update(null, u);
                    mediaService.deleteByUrl(url);
                } else {
                    videoMapper.update(null, new LambdaUpdateWrapper<Video>().eq(Video::getId, vid)
                            .set(Video::getStatus, 1));
                }
            }
            if (needCover) {
                Video cur = videoMapper.selectById(vid);
                int dur = cur != null && cur.getDuration() != null ? cur.getDuration() : 0;
                int sec = Math.max(0, Math.min(3, dur / 3));
                String c = mediaService.generateCover(playUrl, sec);
                if (c != null) {
                    videoMapper.update(null, new LambdaUpdateWrapper<Video>().eq(Video::getId, vid).set(Video::getCover, c));
                }
            }
        } catch (Exception e) {
            log.error("postProcess error {}", vid, e);
        }
    }

    private int parseInt(String s) {
        try {
            return (int) Double.parseDouble(s);
        } catch (Exception e) {
            return 0;
        }
    }

    // ---------------------------------------------------------------- 三连

    public Vos.ToggleResult toggleLike(Long videoId) {
        Long me = UserContext.requireId();
        Video v = mustExist(videoId);
        LambdaQueryWrapper<VideoLike> w = new LambdaQueryWrapper<VideoLike>()
                .eq(VideoLike::getVideoId, videoId).eq(VideoLike::getUserId, me);
        VideoLike exist = likeMapper.selectOne(w);
        Vos.ToggleResult r = new Vos.ToggleResult();
        if (exist != null) {
            likeMapper.deleteById(exist.getId());
            videoMapper.update(null, new LambdaUpdateWrapper<Video>().eq(Video::getId, videoId)
                    .setSql("like_count = GREATEST(like_count - 1, 0)"));
            r.setActive(false);
            r.setMsg("已取消点赞");
        } else {
            VideoLike like = new VideoLike();
            like.setVideoId(videoId);
            like.setUserId(me);
            likeMapper.insert(like);
            videoMapper.update(null, new LambdaUpdateWrapper<Video>().eq(Video::getId, videoId)
                    .setSql("like_count = like_count + 1"));
            r.setActive(true);
            r.setMsg("点赞成功");
            notificationService.push(v.getUserId(), me, "like", videoId, "赞了你的视频");
        }
        r.setCount(nz(likeMapper.selectCount(new LambdaQueryWrapper<VideoLike>().eq(VideoLike::getVideoId, videoId))));
        return r;
    }

    public Vos.ToggleResult toggleCoin(Long videoId) {
        Long me = UserContext.requireId();
        Video v = mustExist(videoId);
        if (Objects.equals(v.getUserId(), me)) throw new BizException("不能给自己的视频投币");
        if (coinMapper.selectCount(new LambdaQueryWrapper<VideoCoin>()
                .eq(VideoCoin::getVideoId, videoId).eq(VideoCoin::getUserId, me)) > 0) {
            throw new BizException("你已经投过币啦");
        }
        User meUser = userMapper.selectById(me);
        if (meUser.getCoin() == null || meUser.getCoin() < 1) throw new BizException("硬币不足，无法投币");
        userMapper.update(null, new LambdaUpdateWrapper<User>().eq(User::getId, me).setSql("coin = coin - 1"));
        userMapper.update(null, new LambdaUpdateWrapper<User>().eq(User::getId, v.getUserId()).setSql("coin = coin + 1"));
        VideoCoin c = new VideoCoin();
        c.setVideoId(videoId);
        c.setUserId(me);
        c.setAmount(1);
        coinMapper.insert(c);
        videoMapper.update(null, new LambdaUpdateWrapper<Video>().eq(Video::getId, videoId)
                .setSql("coin_count = coin_count + 1"));
        notificationService.push(v.getUserId(), me, "like", videoId, "给你的视频投了 1 枚硬币");
        Vos.ToggleResult r = new Vos.ToggleResult();
        r.setActive(true);
        r.setMsg("投币成功");
        r.setCount(nz(coinMapper.selectCount(new LambdaQueryWrapper<VideoCoin>().eq(VideoCoin::getVideoId, videoId))));
        return r;
    }

    public Vos.ToggleResult toggleFav(Long videoId) {
        Long me = UserContext.requireId();
        mustExist(videoId);
        LambdaQueryWrapper<VideoFav> w = new LambdaQueryWrapper<VideoFav>()
                .eq(VideoFav::getVideoId, videoId).eq(VideoFav::getUserId, me);
        VideoFav exist = favMapper.selectOne(w);
        Vos.ToggleResult r = new Vos.ToggleResult();
        if (exist != null) {
            favMapper.deleteById(exist.getId());
            videoMapper.update(null, new LambdaUpdateWrapper<Video>().eq(Video::getId, videoId)
                    .setSql("fav_count = GREATEST(fav_count - 1, 0)"));
            r.setActive(false);
            r.setMsg("已取消收藏");
        } else {
            VideoFav f = new VideoFav();
            f.setVideoId(videoId);
            f.setUserId(me);
            favMapper.insert(f);
            videoMapper.update(null, new LambdaUpdateWrapper<Video>().eq(Video::getId, videoId)
                    .setSql("fav_count = fav_count + 1"));
            r.setActive(true);
            r.setMsg("收藏成功");
        }
        r.setCount(nz(favMapper.selectCount(new LambdaQueryWrapper<VideoFav>().eq(VideoFav::getVideoId, videoId))));
        return r;
    }

    // ---------------------------------------------------------------- 个人中心

    public PageResult<Vos.VideoVO> byUser(Long userId, int page, int size, boolean includeMine) {
        Long me = UserContext.id();
        boolean mine = me != null && me.equals(userId);
        LambdaQueryWrapper<Video> w = new LambdaQueryWrapper<Video>().eq(Video::getUserId, userId);
        if (!(mine && includeMine)) w.eq(Video::getStatus, 1);
        w.orderByDesc(Video::getCreatedAt);
        Page<Video> p = videoMapper.selectPage(new Page<>(page, size), w);
        return PageResult.of(toVOs(p.getRecords()), p.getTotal(), page, size);
    }

    public PageResult<Vos.VideoVO> relationList(String type, int page, int size) {
        Long me = UserContext.requireId();
        List<Long> ids = new ArrayList<>();
        long total = 0;
        if ("history".equals(type)) {
            Page<History> p = historyMapper.selectPage(new Page<>(page, size),
                    new LambdaQueryWrapper<History>().eq(History::getUserId, me).orderByDesc(History::getUpdatedAt));
            ids = p.getRecords().stream().map(History::getVideoId).toList();
            total = p.getTotal();
        } else if ("fav".equals(type)) {
            Page<VideoFav> p = favMapper.selectPage(new Page<>(page, size),
                    new LambdaQueryWrapper<VideoFav>().eq(VideoFav::getUserId, me).orderByDesc(VideoFav::getId));
            ids = p.getRecords().stream().map(VideoFav::getVideoId).toList();
            total = p.getTotal();
        } else if ("like".equals(type)) {
            Page<VideoLike> p = likeMapper.selectPage(new Page<>(page, size),
                    new LambdaQueryWrapper<VideoLike>().eq(VideoLike::getUserId, me).orderByDesc(VideoLike::getId));
            ids = p.getRecords().stream().map(VideoLike::getVideoId).toList();
            total = p.getTotal();
        }
        if (ids.isEmpty()) return PageResult.of(List.of(), total, page, size);
        Map<Long, Video> map = videoMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(Video::getId, Function.identity()));
        List<Video> ordered = ids.stream().map(map::get).filter(Objects::nonNull).toList();
        return PageResult.of(toVOs(ordered), total, page, size);
    }

    public void recordHistory(Long videoId, Integer progress) {
        Long me = UserContext.id();
        if (me == null) return;
        History h = historyMapper.selectOne(new LambdaQueryWrapper<History>()
                .eq(History::getUserId, me).eq(History::getVideoId, videoId));
        if (h == null) {
            h = new History();
            h.setUserId(me);
            h.setVideoId(videoId);
            h.setProgress(progress == null ? 0 : progress);
            historyMapper.insert(h);
        } else {
            h.setProgress(progress == null ? h.getProgress() : progress);
            h.setUpdatedAt(java.time.LocalDateTime.now());
            historyMapper.updateById(h);
        }
    }

    public void clearHistory() {
        Long me = UserContext.requireId();
        historyMapper.delete(new LambdaQueryWrapper<History>().eq(History::getUserId, me));
    }

    public void deleteHistory(Long videoId) {
        Long me = UserContext.requireId();
        historyMapper.delete(new LambdaQueryWrapper<History>()
                .eq(History::getUserId, me).eq(History::getVideoId, videoId));
    }

    public void delete(Long videoId) {
        Long me = UserContext.requireId();
        Video v = mustExist(videoId);
        if (!Objects.equals(v.getUserId(), me) && !UserContext.isAdmin()) throw BizException.forbidden();
        videoMapper.deleteById(videoId);
        mediaService.deleteByUrl(v.getVideoUrl());
        mediaService.deleteByUrl(v.getCover());
    }

    // ---------------------------------------------------------------- 后台

    public void adminSetStatus(Long videoId, int status) {
        videoMapper.update(null, new LambdaUpdateWrapper<Video>().eq(Video::getId, videoId).set(Video::getStatus, status));
    }

    // ---------------------------------------------------------------- 工具

    private Video mustExist(Long id) {
        Video v = videoMapper.selectById(id);
        if (v == null) throw BizException.notFound("视频");
        return v;
    }

    private long nz(Long v) {
        return v == null ? 0 : v;
    }

    public List<Vos.VideoVO> toVOs(List<Video> list) {
        if (list == null || list.isEmpty()) return new ArrayList<>();
        Set<Long> uids = list.stream().map(Video::getUserId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, User> users = uids.isEmpty() ? Map.of()
                : userMapper.selectBatchIds(uids).stream().collect(Collectors.toMap(User::getId, Function.identity()));
        Map<Long, String> cats = categoryMapper.selectList(null).stream()
                .collect(Collectors.toMap(Category::getId, Category::getName, (a, b) -> a));
        Long me = UserContext.id();
        Set<Long> liked = new HashSet<>(), coined = new HashSet<>(), faved = new HashSet<>();
        List<Long> vids = list.stream().map(Video::getId).toList();
        if (me != null && !vids.isEmpty()) {
            likeMapper.selectList(new LambdaQueryWrapper<VideoLike>().eq(VideoLike::getUserId, me).in(VideoLike::getVideoId, vids))
                    .forEach(x -> liked.add(x.getVideoId()));
            coinMapper.selectList(new LambdaQueryWrapper<VideoCoin>().eq(VideoCoin::getUserId, me).in(VideoCoin::getVideoId, vids))
                    .forEach(x -> coined.add(x.getVideoId()));
            favMapper.selectList(new LambdaQueryWrapper<VideoFav>().eq(VideoFav::getUserId, me).in(VideoFav::getVideoId, vids))
                    .forEach(x -> faved.add(x.getVideoId()));
        }
        List<Vos.VideoVO> out = new ArrayList<>(list.size());
        for (Video v : list) {
            Vos.VideoVO vo = new Vos.VideoVO();
            vo.setId(v.getId());
            vo.setTitle(v.getTitle());
            vo.setDescription(v.getDescription());
            vo.setCover(v.getCover());
            vo.setVideoUrl(v.getVideoUrl());
            vo.setDuration(v.getDuration());
            vo.setWidth(v.getWidth());
            vo.setHeight(v.getHeight());
            vo.setStatus(v.getStatus());
            vo.setViews(nz(v.getViews()));
            vo.setDanmakuCount(nz(v.getDanmakuCount()));
            vo.setLikeCount(nz(v.getLikeCount()));
            vo.setCoinCount(nz(v.getCoinCount()));
            vo.setFavCount(nz(v.getFavCount()));
            vo.setCommentCount(nz(v.getCommentCount()));
            vo.setTags(v.getTags());
            vo.setCreatedAt(v.getCreatedAt());
            vo.setCategoryId(v.getCategoryId());
            vo.setCategoryName(cats.get(v.getCategoryId()));
            vo.setAuthor(userService.basic(users.get(v.getUserId())));
            vo.setLiked(liked.contains(v.getId()));
            vo.setCoined(coined.contains(v.getId()));
            vo.setFaved(faved.contains(v.getId()));
            vo.setMine(me != null && me.equals(v.getUserId()));
            out.add(vo);
        }
        return out;
    }
}
