package com.ciyuan.controller;

import com.ciyuan.common.ApiResponse;
import com.ciyuan.common.Login;
import com.ciyuan.common.PageResult;
import com.ciyuan.dto.LiveRequests;
import com.ciyuan.service.LiveService;
import com.ciyuan.vo.LiveVos;
import com.ciyuan.vo.Vos;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/live")
@RequiredArgsConstructor
public class LiveController {

    private final LiveService liveService;

    // ---------------- 直播间

    @GetMapping("/rooms")
    public ApiResponse<PageResult<LiveVos.LiveRoomVO>> rooms(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "24") int size,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Long categoryId) {
        return ApiResponse.ok(liveService.page(page, size, status, categoryId));
    }

    @GetMapping("/rooms/{id}")
    public ApiResponse<LiveVos.LiveRoomVO> detail(@PathVariable Long id) {
        return ApiResponse.ok(liveService.detail(id));
    }

    @Login
    @GetMapping("/my")
    public ApiResponse<LiveVos.LiveRoomVO> my() {
        return ApiResponse.ok(liveService.my());
    }

    @Login
    @PostMapping("/rooms")
    public ApiResponse<LiveVos.LiveRoomVO> create(@Valid @RequestBody LiveRequests.CreateRoom req) {
        return ApiResponse.ok(liveService.create(req));
    }

    @Login
    @PutMapping("/rooms/{id}")
    public ApiResponse<LiveVos.LiveRoomVO> update(@PathVariable Long id,
                                                  @Valid @RequestBody LiveRequests.CreateRoom req) {
        return ApiResponse.ok(liveService.update(id, req));
    }

    @Login
    @PostMapping("/rooms/{id}/start")
    public ApiResponse<LiveVos.LiveRoomVO> start(@PathVariable Long id) {
        return ApiResponse.ok(liveService.start(id));
    }

    @Login
    @PostMapping("/rooms/{id}/stop")
    public ApiResponse<LiveVos.LiveRoomVO> stop(@PathVariable Long id) {
        return ApiResponse.ok(liveService.stop(id));
    }

    @Login
    @DeleteMapping("/rooms/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        liveService.delete(id);
        return ApiResponse.ok();
    }

    @Login
    @PostMapping("/rooms/{id}/like")
    public ApiResponse<Vos.ToggleResult> like(@PathVariable Long id) {
        return ApiResponse.ok(liveService.like(id));
    }

    // ---------------- 在线

    @Login
    @PostMapping("/rooms/{id}/heartbeat")
    public ApiResponse<LiveVos.OnlineVO> heartbeat(@PathVariable Long id) {
        LiveVos.OnlineVO vo = new LiveVos.OnlineVO();
        vo.setViewers(liveService.heartbeat(id));
        return ApiResponse.ok(vo);
    }

    @GetMapping("/rooms/{id}/online")
    public ApiResponse<LiveVos.OnlineVO> online(@PathVariable Long id) {
        LiveVos.OnlineVO vo = new LiveVos.OnlineVO();
        vo.setViewers(liveService.onlineCount(id));
        return ApiResponse.ok(vo);
    }

    // ---------------- 消息

    @GetMapping("/rooms/{id}/messages")
    public ApiResponse<List<LiveVos.LiveMessageVO>> messages(
            @PathVariable Long id,
            @RequestParam(required = false) Long afterId,
            @RequestParam(defaultValue = "50") int limit) {
        return ApiResponse.ok(liveService.messages(id, afterId, limit));
    }

    @Login
    @PostMapping("/rooms/{id}/messages")
    public ApiResponse<LiveVos.LiveMessageVO> send(@PathVariable Long id,
                                                   @Valid @RequestBody LiveRequests.MessageReq req) {
        return ApiResponse.ok(liveService.send(id, req));
    }
}
