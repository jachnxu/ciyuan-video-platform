package com.ciyuan.controller;

import com.ciyuan.common.ApiResponse;
import com.ciyuan.common.Login;
import com.ciyuan.common.PageResult;
import com.ciyuan.dto.Requests;
import com.ciyuan.service.VideoService;
import com.ciyuan.vo.Vos;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/videos")
@RequiredArgsConstructor
public class VideoController {

    private final VideoService videoService;

    @GetMapping
    public ApiResponse<PageResult<Vos.VideoVO>> list(@RequestParam(defaultValue = "1") int page,
                                                     @RequestParam(defaultValue = "24") int size,
                                                     @RequestParam(required = false) Long categoryId,
                                                     @RequestParam(required = false) String keyword,
                                                     @RequestParam(defaultValue = "new") String sort,
                                                     @RequestParam(required = false) Long userId) {
        return ApiResponse.ok(videoService.page(page, size, categoryId, keyword, sort, userId, null));
    }

    @GetMapping("/{id}")
    public ApiResponse<Vos.VideoVO> detail(@PathVariable Long id) {
        return ApiResponse.ok(videoService.detail(id));
    }

    @GetMapping("/{id}/related")
    public ApiResponse<List<Vos.VideoVO>> related(@PathVariable Long id,
                                                  @RequestParam(defaultValue = "10") int limit) {
        return ApiResponse.ok(videoService.related(id, limit));
    }

    @Login
    @PostMapping("/{id}/like")
    public ApiResponse<Vos.ToggleResult> like(@PathVariable Long id) {
        return ApiResponse.ok(videoService.toggleLike(id));
    }

    @Login
    @PostMapping("/{id}/coin")
    public ApiResponse<Vos.ToggleResult> coin(@PathVariable Long id) {
        return ApiResponse.ok(videoService.toggleCoin(id));
    }

    @Login
    @PostMapping("/{id}/fav")
    public ApiResponse<Vos.ToggleResult> fav(@PathVariable Long id) {
        return ApiResponse.ok(videoService.toggleFav(id));
    }

    @PostMapping("/{id}/history")
    public ApiResponse<Void> history(@PathVariable Long id,
                                     @RequestParam(required = false) Integer progress) {
        videoService.recordHistory(id, progress);
        return ApiResponse.ok();
    }

    @Login
    @PostMapping
    public ApiResponse<Vos.VideoVO> publish(@RequestParam("file") MultipartFile file,
                                            @RequestParam(value = "cover", required = false) MultipartFile cover,
                                            @Valid Requests.Publish req) {
        return ApiResponse.ok(videoService.publish(file, cover, req));
    }

    @Login
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        videoService.delete(id);
        return ApiResponse.ok();
    }
}
