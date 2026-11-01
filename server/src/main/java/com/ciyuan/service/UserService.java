package com.ciyuan.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ciyuan.common.BizException;
import com.ciyuan.common.PageResult;
import com.ciyuan.common.UserContext;
import com.ciyuan.dto.Requests;
import com.ciyuan.entity.Follow;
import com.ciyuan.entity.User;
import com.ciyuan.entity.Video;
import com.ciyuan.mapper.FollowMapper;
import com.ciyuan.mapper.UserMapper;
import com.ciyuan.mapper.VideoMapper;
import com.ciyuan.vo.Vos;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;
    private final VideoMapper videoMapper;
    private final FollowMapper followMapper;
    private final BCryptPasswordEncoder encoder;
    private final MediaService mediaService;

    public User get(Long id) {
        User u = userMapper.selectById(id);
        if (u == null) throw BizException.notFound("用户");
        return u;
    }

    public Vos.UserVO basic(User u) {
        if (u == null) return null;
        Vos.UserVO vo = new Vos.UserVO();
        vo.setId(u.getId());
        vo.setUsername(u.getUsername());
        vo.setNickname(u.getNickname());
        vo.setAvatar(u.getAvatar());
        vo.setSign(u.getSign());
        vo.setGender(u.getGender());
        vo.setLevel(u.getLevel());
        vo.setRole(u.getRole());
        vo.setCreatedAt(u.getCreatedAt());
        return vo;
    }

    public Vos.UserVO profile(Long id) {
        User u = get(id);
        Vos.UserVO vo = basic(u);
        vo.setCoin(u.getCoin());
        vo.setFollowers(followMapper.selectCount(new LambdaQueryWrapper<Follow>().eq(Follow::getFollowingId, id)));
        vo.setFollowing(followMapper.selectCount(new LambdaQueryWrapper<Follow>().eq(Follow::getFollowerId, id)));
        List<Video> videos = videoMapper.selectList(new LambdaQueryWrapper<Video>()
                .eq(Video::getUserId, id).eq(Video::getStatus, 1)
                .select(Video::getId, Video::getViews));
        vo.setVideoCount((long) videos.size());
        vo.setTotalViews(videos.stream().mapToLong(v -> v.getViews() == null ? 0 : v.getViews()).sum());
        Long me = UserContext.id();
        if (me != null) {
            vo.setFollowed(followMapper.selectCount(new LambdaQueryWrapper<Follow>()
                    .eq(Follow::getFollowerId, me).eq(Follow::getFollowingId, id)) > 0);
        } else {
            vo.setFollowed(false);
        }
        return vo;
    }

    public Vos.UserVO me() {
        return profile(UserContext.requireId());
    }

    public Vos.UserVO updateMe(Requests.ProfileReq req) {
        Long me = UserContext.requireId();
        User u = get(me);
        if (StringUtils.hasText(req.getNickname())) {
            if (req.getNickname().length() > 20) throw new BizException("昵称最多 20 字");
            u.setNickname(req.getNickname().trim());
        }
        if (req.getSign() != null) u.setSign(req.getSign().length() > 100 ? req.getSign().substring(0, 100) : req.getSign());
        if (req.getAvatar() != null) u.setAvatar(req.getAvatar());
        if (req.getGender() != null) u.setGender(req.getGender());
        userMapper.updateById(u);
        return profile(me);
    }

    public Vos.UserVO uploadAvatar(MultipartFile file) {
        Long me = UserContext.requireId();
        String url = mediaService.save(file, "avatar");
        User u = get(me);
        u.setAvatar(url);
        userMapper.updateById(u);
        return basic(u);
    }

    public void changePassword(Requests.PasswordReq req) {
        User u = get(UserContext.requireId());
        if (!encoder.matches(req.getOldPassword(), u.getPassword())) throw new BizException("原密码不正确");
        u.setPassword(encoder.encode(req.getNewPassword()));
        userMapper.updateById(u);
    }

    public Vos.ToggleResult toggleFollow(Long targetId) {
        Long me = UserContext.requireId();
        if (me.equals(targetId)) throw new BizException("不能关注自己");
        get(targetId);
        LambdaQueryWrapper<Follow> w = new LambdaQueryWrapper<Follow>()
                .eq(Follow::getFollowerId, me).eq(Follow::getFollowingId, targetId);
        Follow exist = followMapper.selectOne(w);
        Vos.ToggleResult r = new Vos.ToggleResult();
        if (exist != null) {
            followMapper.deleteById(exist.getId());
            r.setActive(false);
            r.setMsg("已取消关注");
        } else {
            Follow f = new Follow();
            f.setFollowerId(me);
            f.setFollowingId(targetId);
            followMapper.insert(f);
            r.setActive(true);
            r.setMsg("关注成功");
        }
        r.setCount(followMapper.selectCount(new LambdaQueryWrapper<Follow>().eq(Follow::getFollowingId, targetId)));
        return r;
    }

    public PageResult<Vos.UserVO> followList(Long userId, String type, int page, int size) {
        Page<Follow> p;
        if ("following".equals(type)) {
            p = followMapper.selectPage(new Page<>(page, size), new LambdaQueryWrapper<Follow>()
                    .eq(Follow::getFollowerId, userId).orderByDesc(Follow::getId));
            List<Vos.UserVO> list = p.getRecords().stream().map(f -> {
                Vos.UserVO vo = basic(get(f.getFollowingId()));
                vo.setFollowed(true);
                return vo;
            }).toList();
            return PageResult.of(list, p.getTotal(), page, size);
        }
        p = followMapper.selectPage(new Page<>(page, size), new LambdaQueryWrapper<Follow>()
                .eq(Follow::getFollowingId, userId).orderByDesc(Follow::getId));
        Long me = UserContext.id();
        List<Vos.UserVO> list = p.getRecords().stream().map(f -> {
            Vos.UserVO vo = basic(get(f.getFollowerId()));
            vo.setFollowed(me != null && followMapper.selectCount(new LambdaQueryWrapper<Follow>()
                    .eq(Follow::getFollowerId, me).eq(Follow::getFollowingId, f.getFollowerId())) > 0);
            return vo;
        }).toList();
        return PageResult.of(list, p.getTotal(), page, size);
    }

    public PageResult<Vos.UserVO> search(String keyword, int page, int size) {
        Page<User> p = userMapper.selectPage(new Page<>(page, size), new LambdaQueryWrapper<User>()
                .eq(User::getStatus, 0)
                .and(w -> w.like(User::getNickname, keyword).or().like(User::getUsername, keyword))
                .orderByDesc(User::getId));
        List<Vos.UserVO> list = p.getRecords().stream().map(this::basic).toList();
        return PageResult.of(list, p.getTotal(), page, size);
    }
}
