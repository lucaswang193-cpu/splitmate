package com.splitmate.controller;

import lombok.RequiredArgsConstructor;

import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.splitmate.dto.expense.ExpenseShareResponse;
import com.splitmate.response.ApiResponse;
import com.splitmate.response.PageResponse;
import com.splitmate.service.ExpenseShareService;


@RestController
@RequestMapping("/api/expense-shares")
@RequiredArgsConstructor
public class ExpenseShareController {

    private final ExpenseShareService expenseShareService;
    @GetMapping("/expenses/{expenseId}")
    public ResponseEntity<ApiResponse<PageResponse<ExpenseShareResponse>>> getSharesByExpenseId(
            @PathVariable UUID expenseId,
            Pageable pageable
    ) {
        return ResponseEntity.ok(ApiResponse.success(expenseShareService.getSharesByExpenseId(expenseId, pageable)));
    }

    @GetMapping("/users/{userId}")
    public ResponseEntity<ApiResponse<PageResponse<ExpenseShareResponse>>> getSharesByUserId(
            @PathVariable UUID userId,
            Pageable pageable
    ) {
        return ResponseEntity.ok(ApiResponse.success(expenseShareService.getSharesByUserId(userId, pageable)));
    }
}
