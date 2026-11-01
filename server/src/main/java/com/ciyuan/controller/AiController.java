package com.ciyuan.controller;

import com.ciyuan.common.ApiResponse;
import com.ciyuan.dto.AiRequests;
import com.ciyuan.service.AiService;
import com.ciyuan.vo.AiVos;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 大模型视频推荐。免登录即可使用（游客也能问），
 * 已登录时返回的视频会带上个人点赞/投币/收藏状态。
 */
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService;

    @PostMapping("/chat")
    public ApiResponse<AiVos.ChatResult> chat(@Valid @RequestBody AiRequests.Chat req) {
        return ApiResponse.ok(aiService.chat(req));
    }

    /** 前端据此判断是否要提示「AI 未配置」 */
    @GetMapping("/status")
    public ApiResponse<Map<String, Object>> status() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("configured", aiService.configured());
        m.put("model", aiService.modelName());
        return ApiResponse.ok(m);
    }
}
