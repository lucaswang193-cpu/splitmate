package com.splitmate.controller;

import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.splitmate.dto.statistics.GroupExpenseStatsResponse;
import com.splitmate.response.ApiResponse;
import com.splitmate.service.ExpenseStatisticsService;


@RestController
@RequestMapping("/api/expenses/groups")
@RequiredArgsConstructor
public class ExpenseStatisticsController {

    private final ExpenseStatisticsService expenseStatisticsService;
    @GetMapping("/{groupId}/category-stats")
    public ResponseEntity<ApiResponse<GroupExpenseStatsResponse>> getCategoryStats(
            @PathVariable UUID groupId,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime from,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime to
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                expenseStatisticsService.getCategoryStats(groupId, from, to)
        ));
    }
}
