package com.ciyuan.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.ciyuan.common.BizException;
import com.ciyuan.common.UserContext;
import com.ciyuan.dto.Requests;
import com.ciyuan.entity.Danmaku;
import com.ciyuan.entity.Video;
import com.ciyuan.mapper.DanmakuMapper;
import com.ciyuan.mapper.VideoMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DanmakuService {

    private final DanmakuMapper danmakuMapper;
    private final VideoMapper videoMapper;

    public List<Map<String, Object>> list(Long videoId) {
        List<Danmaku> list = danmakuMapper.selectList(new LambdaQueryWrapper<Danmaku>()
                .eq(Danmaku::getVideoId, videoId).orderByAsc(Danmaku::getTimeSec).last("limit 5000"));
        return list.stream().map(d -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", d.getId());
            m.put("text", d.getContent());
            m.put("time", d.getTimeSec());
            m.put("color", d.getColor());
            m.put("userId", d.getUserId());
            return m;
        }).toList();
    }

    public Map<String, Object> add(Requests.DanmakuReq req) {
        Long me = UserContext.requireId();
        Video v = videoMapper.selectById(req.getVideoId());
        if (v == null) throw BizException.notFound("视频");
        Danmaku d = new Danmaku();
        d.setVideoId(req.getVideoId());
        d.setUserId(me);
        d.setContent(req.getContent().trim());
        d.setTimeSec(req.getTime() == null ? 0 : Math.max(0, req.getTime()));
        d.setColor(StringUtils.hasText(req.getColor()) ? req.getColor() : "#ffffff");
        danmakuMapper.insert(d);
        videoMapper.update(null, new LambdaUpdateWrapper<Video>().eq(Video::getId, req.getVideoId())
                .setSql("danmaku_count = danmaku_count + 1"));
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", d.getId());
        m.put("text", d.getContent());
        m.put("time", d.getTimeSec());
        m.put("color", d.getColor());
        m.put("userId", me);
        return m;
    }
}
