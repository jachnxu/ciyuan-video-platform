package com.ciyuan.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.ciyuan.common.JwtUtil;
import com.ciyuan.entity.LiveRoom;
import com.ciyuan.mapper.LiveRoomMapper;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.BinaryWebSocketHandler;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.ByteBuffer;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class LiveStreamHandler extends BinaryWebSocketHandler {

    private final JwtUtil jwtUtil;
    private final LiveRoomMapper roomMapper;

    private static final Map<String, FfmpegStream> STREAMS = new ConcurrentHashMap<>();
    private static final String ATTR_KEY = "liveStreamKey";

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String key = null;
        String token = null;
        String fmt = "webm";
        URI uri = session.getUri();
        if (uri != null) {
            String path = uri.getPath();
            if (path != null && path.contains("/")) {
                key = path.substring(path.lastIndexOf('/') + 1);
            }
            String q = uri.getRawQuery();
            if (q != null) {
                for (String kv : q.split("&")) {
                    int i = kv.indexOf('=');
                    if (i <= 0) continue;
                    String k = kv.substring(0, i);
                    String v = URLDecoder.decode(kv.substring(i + 1), "UTF-8");
                    if ("token".equals(k)) token = v;
                    else if ("fmt".equals(k)) fmt = v;
                }
            }
        }

        if (key == null || key.isBlank() || token == null) {
            session.close(CloseStatus.BAD_DATA);
            return;
        }

        Long uid;
        try {
            Claims c = jwtUtil.parse(token);
            uid = Long.valueOf(c.getSubject());
        } catch (Exception e) {
            session.close(CloseStatus.NOT_ACCEPTABLE);
            return;
        }

        LiveRoom room = roomMapper.selectOne(
                new LambdaQueryWrapper<LiveRoom>().eq(LiveRoom::getStreamKey, key));
        if (room == null || room.getUserId() == null || !room.getUserId().equals(uid)) {
            session.close(CloseStatus.NOT_ACCEPTABLE);
            return;
        }

        FfmpegStream old = STREAMS.remove(key);
        if (old != null) old.stop();

        FfmpegStream fs = new FfmpegStream(key, fmt);
        fs.start();
        STREAMS.put(key, fs);
        session.getAttributes().put(ATTR_KEY, key);

        roomMapper.update(null, new LambdaUpdateWrapper<LiveRoom>()
                .eq(LiveRoom::getId, room.getId())
                .set(LiveRoom::getStatus, 1)
                .set(LiveRoom::getStartedAt, LocalDateTime.now())
                .set(LiveRoom::getEndedAt, null)
                .set(LiveRoom::getViewers, 0L));

        log.info("browser live start key={} fmt={} uid={}", key, fmt, uid);
    }

    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) {
        String key = (String) session.getAttributes().get(ATTR_KEY);
        if (key == null) return;
        FfmpegStream fs = STREAMS.get(key);
        if (fs == null) return;
        ByteBuffer buf = message.getPayload();
        byte[] data = new byte[buf.remaining()];
        buf.get(data);
        fs.write(data);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String key = (String) session.getAttributes().get(ATTR_KEY);
        if (key == null) return;
        FfmpegStream fs = STREAMS.remove(key);
        if (fs != null) fs.stop();

        LiveRoom room = roomMapper.selectOne(
                new LambdaQueryWrapper<LiveRoom>().eq(LiveRoom::getStreamKey, key));
        if (room != null) {
            roomMapper.update(null, new LambdaUpdateWrapper<LiveRoom>()
                    .eq(LiveRoom::getId, room.getId())
                    .set(LiveRoom::getStatus, 2)
                    .set(LiveRoom::getEndedAt, LocalDateTime.now())
                    .set(LiveRoom::getViewers, 0L));
        }
        log.info("browser live stop key={}", key);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.warn("ws transport error: {}", exception.getMessage());
        afterConnectionClosed(session, CloseStatus.SERVER_ERROR);
    }
}
