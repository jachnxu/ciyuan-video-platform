package com.ciyuan.controller;

import com.ciyuan.common.ApiResponse;
import com.ciyuan.common.Login;
import com.ciyuan.dto.Requests;
import com.ciyuan.service.DanmakuService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/danmaku")
@RequiredArgsConstructor
public class DanmakuController {

    private final DanmakuService danmakuService;

    @GetMapping("/{videoId}")
    public ApiResponse<List<Map<String, Object>>> list(@PathVariable Long videoId) {
        return ApiResponse.ok(danmakuService.list(videoId));
    }

    @Login
    @PostMapping
    public ApiResponse<Map<String, Object>> add(@Valid @RequestBody Requests.DanmakuReq req) {
        return ApiResponse.ok(danmakuService.add(req));
    }
}
