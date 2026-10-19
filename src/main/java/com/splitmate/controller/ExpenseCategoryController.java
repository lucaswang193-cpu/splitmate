package com.splitmate.controller;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.splitmate.dto.category.ExpenseCategoryResponse;
import com.splitmate.response.ApiResponse;
import com.splitmate.response.PageResponse;
import com.splitmate.service.ExpenseCategoryService;


@RestController
@RequestMapping("/api/expense-categories")
@RequiredArgsConstructor
public class ExpenseCategoryController {

    private final ExpenseCategoryService expenseCategoryService;
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ExpenseCategoryResponse>>> getAllCategories(Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(expenseCategoryService.getAllCategories(pageable)));
    }
}
