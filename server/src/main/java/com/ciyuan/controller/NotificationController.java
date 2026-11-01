package com.ciyuan.controller;

import com.ciyuan.common.ApiResponse;
import com.ciyuan.common.Login;
import com.ciyuan.common.PageResult;
import com.ciyuan.service.NotificationService;
import com.ciyuan.vo.Vos;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Login
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ApiResponse<PageResult<Vos.NotifVO>> list(@RequestParam(defaultValue = "1") int page,
                                                     @RequestParam(defaultValue = "15") int size) {
        return ApiResponse.ok(notificationService.list(page, size));
    }

    @GetMapping("/unread")
    public ApiResponse<Map<String, Long>> unread() {
        return ApiResponse.ok(Map.of("count", notificationService.unread()));
    }

    @PostMapping("/read")
    public ApiResponse<Void> read() {
        notificationService.readAll();
        return ApiResponse.ok();
    }
}
