package com.ciyuan.controller;

import com.ciyuan.common.ApiResponse;
import com.ciyuan.common.Login;
import com.ciyuan.common.PageResult;
import com.ciyuan.dto.Requests;
import com.ciyuan.service.UserService;
import com.ciyuan.service.VideoService;
import com.ciyuan.vo.Vos;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final VideoService videoService;

    @GetMapping("/users/{id}")
    public ApiResponse<Vos.UserVO> profile(@PathVariable Long id) {
        return ApiResponse.ok(userService.profile(id));
    }

    @GetMapping("/users/{id}/videos")
    public ApiResponse<PageResult<Vos.VideoVO>> videos(@PathVariable Long id,
                                                       @RequestParam(defaultValue = "1") int page,
                                                       @RequestParam(defaultValue = "12") int size) {
        return ApiResponse.ok(videoService.byUser(id, page, size, true));
    }

    @GetMapping("/users/{id}/followers")
    public ApiResponse<PageResult<Vos.UserVO>> followers(@PathVariable Long id,
                                                         @RequestParam(defaultValue = "1") int page,
                                                         @RequestParam(defaultValue = "12") int size) {
        return ApiResponse.ok(userService.followList(id, "followers", page, size));
    }

    @GetMapping("/users/{id}/following")
    public ApiResponse<PageResult<Vos.UserVO>> following(@PathVariable Long id,
                                                         @RequestParam(defaultValue = "1") int page,
                                                         @RequestParam(defaultValue = "12") int size) {
        return ApiResponse.ok(userService.followList(id, "following", page, size));
    }

    @Login
    @PostMapping("/users/{id}/follow")
    public ApiResponse<Vos.ToggleResult> follow(@PathVariable Long id) {
        return ApiResponse.ok(userService.toggleFollow(id));
    }

    @Login
    @PutMapping("/users/me")
    public ApiResponse<Vos.UserVO> update(@RequestBody Requests.ProfileReq req) {
        return ApiResponse.ok(userService.updateMe(req));
    }

    @Login
    @PostMapping("/users/me/avatar")
    public ApiResponse<Vos.UserVO> avatar(@RequestParam("file") MultipartFile file) {
        return ApiResponse.ok(userService.uploadAvatar(file));
    }

    @Login
    @PutMapping("/users/me/password")
    public ApiResponse<Void> password(@Valid @RequestBody Requests.PasswordReq req) {
        userService.changePassword(req);
        return ApiResponse.ok();
    }

    @Login
    @GetMapping("/users/me/videos")
    public ApiResponse<PageResult<Vos.VideoVO>> myVideos(@RequestParam(defaultValue = "1") int page,
                                                         @RequestParam(defaultValue = "12") int size) {
        Long me = com.ciyuan.common.UserContext.requireId();
        return ApiResponse.ok(videoService.byUser(me, page, size, true));
    }

    @Login
    @GetMapping("/users/me/relations")
    public ApiResponse<PageResult<Vos.VideoVO>> relations(@RequestParam(defaultValue = "history") String type,
                                                          @RequestParam(defaultValue = "1") int page,
                                                          @RequestParam(defaultValue = "12") int size) {
        return ApiResponse.ok(videoService.relationList(type, page, size));
    }

    @Login
    @DeleteMapping("/users/me/history")
    public ApiResponse<Void> clearHistory() {
        videoService.clearHistory();
        return ApiResponse.ok();
    }

    @GetMapping("/search")
    public ApiResponse<PageResult<?>> search(@RequestParam String keyword,
                                             @RequestParam(defaultValue = "video") String type,
                                             @RequestParam(defaultValue = "1") int page,
                                             @RequestParam(defaultValue = "12") int size) {
        if ("user".equals(type)) return ApiResponse.ok(userService.search(keyword, page, size));
        return ApiResponse.ok(videoService.page(page, size, null, keyword, "hot", null, null));
    }
}
