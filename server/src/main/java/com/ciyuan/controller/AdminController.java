package com.ciyuan.controller;

import com.ciyuan.common.ApiResponse;
import com.ciyuan.common.Login;
import com.ciyuan.common.PageResult;
import com.ciyuan.entity.Category;
import com.ciyuan.service.AdminService;
import com.ciyuan.service.VideoService;
import com.ciyuan.vo.Vos;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Login(admin = true)
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final VideoService videoService;

    @GetMapping("/stats")
    public ApiResponse<Vos.Stats> stats() {
        return ApiResponse.ok(adminService.stats());
    }

    @GetMapping("/videos")
    public ApiResponse<PageResult<Vos.VideoVO>> videos(@RequestParam(required = false) String keyword,
                                                       @RequestParam(required = false) Integer status,
                                                       @RequestParam(defaultValue = "1") int page,
                                                       @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(adminService.videos(keyword, status, page, size));
    }

    @PostMapping("/videos/{id}/status")
    public ApiResponse<Void> videoStatus(@PathVariable Long id, @RequestParam int status) {
        videoService.adminSetStatus(id, status);
        return ApiResponse.ok();
    }

    @DeleteMapping("/videos/{id}")
    public ApiResponse<Void> deleteVideo(@PathVariable Long id) {
        videoService.delete(id);
        return ApiResponse.ok();
    }

    @GetMapping("/users")
    public ApiResponse<PageResult<Vos.UserVO>> users(@RequestParam(required = false) String keyword,
                                                     @RequestParam(defaultValue = "1") int page,
                                                     @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(adminService.users(keyword, page, size));
    }

    @PostMapping("/users/{id}/status")
    public ApiResponse<Void> userStatus(@PathVariable Long id, @RequestParam int status) {
        adminService.setUserStatus(id, status);
        return ApiResponse.ok();
    }

    @PostMapping("/users/{id}/role")
    public ApiResponse<Void> userRole(@PathVariable Long id, @RequestParam String role) {
        adminService.setUserRole(id, role);
        return ApiResponse.ok();
    }

    @GetMapping("/categories")
    public ApiResponse<List<Category>> categories() {
        return ApiResponse.ok(adminService.categories());
    }

    @PostMapping("/categories")
    public ApiResponse<Category> saveCategory(@RequestBody Category category) {
        return ApiResponse.ok(adminService.saveCategory(category));
    }

    @DeleteMapping("/categories/{id}")
    public ApiResponse<Void> deleteCategory(@PathVariable Long id) {
        adminService.deleteCategory(id);
        return ApiResponse.ok();
    }
}
