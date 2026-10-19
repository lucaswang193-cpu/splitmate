package com.splitmate.controller;

import lombok.RequiredArgsConstructor;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.splitmate.dto.group.CreateGroupRequest;
import com.splitmate.dto.group.GroupPreviewResponse;
import com.splitmate.dto.group.GroupResponse;
import com.splitmate.dto.group.TransferGroupOwnerRequest;
import com.splitmate.dto.group.UpdateGroupRequest;
import com.splitmate.response.ApiResponse;
import com.splitmate.security.SecurityUtils;
import com.splitmate.service.ExpenseGroupService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/groups")
@RequiredArgsConstructor
public class ExpenseGroupController {

    private final ExpenseGroupService expenseGroupService;
    @PostMapping
    public ResponseEntity<ApiResponse<GroupResponse>> createGroup(
            @Valid @RequestBody CreateGroupRequest request
    ) {
        GroupResponse response = expenseGroupService.createGroup(request, SecurityUtils.getCurrentUser().id());
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Group created successfully", response));
    }

    @GetMapping("/{groupId}")
    public ResponseEntity<ApiResponse<GroupResponse>> getGroupById(
            @PathVariable UUID groupId
    ) {
        return ResponseEntity.ok(ApiResponse.success(expenseGroupService.getGroupById(groupId)));
    }

    @GetMapping("/invite/{inviteCode}")
public ResponseEntity<ApiResponse<GroupPreviewResponse>> getGroupByInviteCode(
        @PathVariable String inviteCode) {
    return ResponseEntity.ok(ApiResponse.success(
            expenseGroupService.getGroupByInviteCode(inviteCode)
    ));
}

    @PutMapping("/{groupId}")
    public ResponseEntity<ApiResponse<GroupResponse>> updateGroup(
            @PathVariable UUID groupId,
            @Valid @RequestBody UpdateGroupRequest request
    ) {
        GroupResponse response = expenseGroupService.updateGroup(groupId, request);
        return ResponseEntity.ok(ApiResponse.success("Group updated successfully", response));
    }

    @PutMapping("/{groupId}/owner")
    public ResponseEntity<ApiResponse<GroupResponse>> transferOwner(
            @PathVariable UUID groupId,
            @Valid @RequestBody TransferGroupOwnerRequest request
    ) {
        GroupResponse response = expenseGroupService.transferOwner(groupId, request);
        return ResponseEntity.ok(ApiResponse.success("Group owner transferred successfully", response));
    }
}
