package com.ciyuan.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ciyuan.common.ApiResponse;
import com.ciyuan.entity.Category;
import com.ciyuan.mapper.CategoryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryMapper categoryMapper;

    @GetMapping("/categories")
    public ApiResponse<List<Category>> list() {
        return ApiResponse.ok(categoryMapper.selectList(
                new LambdaQueryWrapper<Category>().orderByAsc(Category::getSort)));
    }
}
