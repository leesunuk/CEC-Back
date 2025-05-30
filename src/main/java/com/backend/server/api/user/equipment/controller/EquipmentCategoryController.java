package com.backend.server.api.user.equipment.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.backend.server.api.common.dto.ApiResponse;
import com.backend.server.api.user.equipment.dto.category.EquipmentCategoryResponse;
import com.backend.server.api.user.equipment.service.EquipmentCategoryService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "장비 카테고리", description = "장비 카테고리 관련 API")
@RestController
@RequestMapping("/api/equipment-categories")
@RequiredArgsConstructor
public class EquipmentCategoryController {

    private final EquipmentCategoryService categoryService;

    @Operation(summary = "전체 카테고리 조회", description = "모든 장비 카테고리를 조회합니다.")
    @GetMapping
    public ApiResponse<List<EquipmentCategoryResponse>> getAllCategories() {
        return ApiResponse.success("카테고리 조회 성공", categoryService.getAllCategories()) ;
    }

    @Operation(summary = "카테고리 상세 조회", description = "특정 ID의 장비 카테고리를 조회합니다.")
    @GetMapping("/{id}")
    public ApiResponse<EquipmentCategoryResponse> getCategoryById(
        @Parameter(description = "카테고리 ID", example = "1") 
        @PathVariable Long id) {
        return ApiResponse.success("특정아이디 장비 카테고리 조회",categoryService.getCategoryById(id));
    }
}