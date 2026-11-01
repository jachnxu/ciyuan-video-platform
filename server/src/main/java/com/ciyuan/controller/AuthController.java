package com.ciyuan.controller;

import com.ciyuan.common.ApiResponse;
import com.ciyuan.dto.Requests;
import com.ciyuan.service.AuthService;
import com.ciyuan.vo.Vos;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ApiResponse<Vos.LoginResult> register(@Valid @RequestBody Requests.Register req) {
        return ApiResponse.ok(authService.register(req));
    }

    @PostMapping("/login")
    public ApiResponse<Vos.LoginResult> login(@Valid @RequestBody Requests.Login req) {
        return ApiResponse.ok(authService.login(req));
    }

    @GetMapping("/me")
    public ApiResponse<Vos.UserVO> me() {
        return ApiResponse.ok(authService.current());
    }
}
