package com.splitmate.controller;

import lombok.RequiredArgsConstructor;

import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.splitmate.dto.balance.BalanceResponse;
import com.splitmate.dto.expense.CreateExpenseRequest;
import com.splitmate.dto.expense.ExpenseResponse;
import com.splitmate.dto.expense.UpdateExpenseRequest;
import com.splitmate.response.ApiResponse;
import com.splitmate.response.PageResponse;
import com.splitmate.service.ExpenseService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/expenses")
@RequiredArgsConstructor
public class ExpenseController {

    private final ExpenseService expenseService;
    @PostMapping
    public ResponseEntity<ApiResponse<ExpenseResponse>> createExpense(
            @Valid @RequestBody CreateExpenseRequest request
    ) {
        ExpenseResponse response = expenseService.createExpense(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Expense created successfully", response));
    }

    @GetMapping("/{expenseId}")
    public ResponseEntity<ApiResponse<ExpenseResponse>> getExpenseById(
            @PathVariable UUID expenseId
    ) {
        return ResponseEntity.ok(ApiResponse.success(expenseService.getExpenseById(expenseId)));
    }

    @PutMapping("/{expenseId}")
    public ResponseEntity<ApiResponse<ExpenseResponse>> updateExpense(
            @PathVariable UUID expenseId,
            @Valid @RequestBody UpdateExpenseRequest request
    ) {
        ExpenseResponse response = expenseService.updateExpense(expenseId, request);
        return ResponseEntity.ok(ApiResponse.success("Expense updated successfully", response));
    }

    @DeleteMapping("/{expenseId}")
    public ResponseEntity<ApiResponse<Void>> deleteExpense(
            @PathVariable UUID expenseId
    ) {
        expenseService.deleteExpense(expenseId);
        return ResponseEntity.ok(ApiResponse.success("Expense deleted successfully", null));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ExpenseResponse>>> getExpenses(
            @RequestParam(required = false) UUID groupId,
            @RequestParam(required = false) UUID payerId,
            Pageable pageable
    ) {
        if (groupId != null) {
            return ResponseEntity.ok(ApiResponse.success(expenseService.getExpensesByGroupId(groupId, pageable)));
        }

        if (payerId != null) {
            return ResponseEntity.ok(ApiResponse.success(expenseService.getExpensesByPayerId(payerId, pageable)));
        }

        return ResponseEntity.badRequest()
                .body(ApiResponse.fail("QUERY_PARAM_REQUIRED", "Either groupId or payerId is required"));
    }

    @GetMapping("/groups/{groupId}/balances")
    public ResponseEntity<ApiResponse<PageResponse<BalanceResponse>>> getGroupBalances(
            @PathVariable UUID groupId,
            Pageable pageable
    ) {
        return ResponseEntity.ok(ApiResponse.success(expenseService.getGroupBalances(groupId, pageable)));
    }
}
