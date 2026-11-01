package com.ciyuan.controller;

import com.ciyuan.common.ApiResponse;
import com.ciyuan.common.Login;
import com.ciyuan.common.PageResult;
import com.ciyuan.dto.Requests;
import com.ciyuan.service.CommentService;
import com.ciyuan.vo.Vos;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @GetMapping
    public ApiResponse<PageResult<Vos.CommentVO>> list(@RequestParam Long videoId,
                                                       @RequestParam(defaultValue = "1") int page,
                                                       @RequestParam(defaultValue = "10") int size,
                                                       @RequestParam(defaultValue = "new") String sort) {
        return ApiResponse.ok(commentService.list(videoId, page, size, sort));
    }

    @GetMapping("/{rootId}/children")
    public ApiResponse<List<Vos.CommentVO>> children(@PathVariable Long rootId) {
        return ApiResponse.ok(commentService.children(rootId));
    }

    @Login
    @PostMapping
    public ApiResponse<Vos.CommentVO> add(@Valid @RequestBody Requests.CommentReq req) {
        return ApiResponse.ok(commentService.add(req));
    }

    @Login
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        commentService.delete(id);
        return ApiResponse.ok();
    }

    @Login
    @PostMapping("/{id}/like")
    public ApiResponse<Vos.ToggleResult> like(@PathVariable Long id) {
        return ApiResponse.ok(commentService.like(id));
    }
}
