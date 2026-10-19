package com.splitmate.controller;

import lombok.RequiredArgsConstructor;

import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.splitmate.dto.settlement.CreateSettlementRequest;
import com.splitmate.dto.settlement.SettlementResponse;
import com.splitmate.response.ApiResponse;
import com.splitmate.response.PageResponse;
import com.splitmate.service.SettlementService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/settlements")
@RequiredArgsConstructor
public class SettlementController {

    private final SettlementService settlementService;
    @PostMapping
    public ResponseEntity<ApiResponse<SettlementResponse>> createSettlement(
            @Valid @RequestBody CreateSettlementRequest request
    ) {
        SettlementResponse response = settlementService.createSettlement(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Settlement created successfully", response));
    }

    @GetMapping("/groups/{groupId}")
    public ResponseEntity<ApiResponse<PageResponse<SettlementResponse>>> getSettlementsByGroupId(
            @PathVariable UUID groupId,
            Pageable pageable
    ) {
        return ResponseEntity.ok(ApiResponse.success(settlementService.getSettlementsByGroupId(groupId, pageable)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<SettlementResponse>>> getSettlementsByUserId(
            @RequestParam UUID userId,
            @RequestParam(defaultValue = "sent") String type,
            Pageable pageable
    ) {
        if ("received".equalsIgnoreCase(type)) {
            return ResponseEntity.ok(ApiResponse.success(settlementService.getReceivedSettlements(userId, pageable)));
        }

        return ResponseEntity.ok(ApiResponse.success(settlementService.getSentSettlements(userId, pageable)));
    }
}
