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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.splitmate.dto.group.GroupMemberResponse;
import com.splitmate.dto.group.JoinGroupRequest;
import com.splitmate.response.ApiResponse;
import com.splitmate.response.PageResponse;
import com.splitmate.security.SecurityUtils;
import com.splitmate.service.GroupMemberService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/group-members")
@RequiredArgsConstructor
public class GroupMemberController {

        private final GroupMemberService groupMemberService;

        @PostMapping("/join")
        public ResponseEntity<ApiResponse<GroupMemberResponse>> joinGroup(
                        @Valid @RequestBody JoinGroupRequest request) {
                GroupMemberResponse response = groupMemberService.joinGroup(request,
                                SecurityUtils.getCurrentUser().id());
                return ResponseEntity
                                .status(HttpStatus.CREATED)
                                .body(ApiResponse.success("Joined group successfully", response));
        }

        @GetMapping("/groups/{groupId}")
        public ResponseEntity<ApiResponse<PageResponse<GroupMemberResponse>>> getMembersByGroupId(
                        @PathVariable UUID groupId,
                        Pageable pageable) {
                return ResponseEntity
                                .ok(ApiResponse.success(groupMemberService.getMembersByGroupId(groupId, pageable)));
        }

        @GetMapping("/users/{userId}")
        public ResponseEntity<ApiResponse<PageResponse<GroupMemberResponse>>> getMembershipsByUserId(
                        @PathVariable UUID userId,
                        Pageable pageable) {
                return ResponseEntity
                                .ok(ApiResponse.success(groupMemberService.getMembershipsByUserId(userId, pageable)));
        }

        @GetMapping("/me")
        public ResponseEntity<ApiResponse<PageResponse<GroupMemberResponse>>> getMyMemberships(
                        Pageable pageable) {
                return ResponseEntity.ok(ApiResponse.success(
                                groupMemberService.getMembershipsByUserId(SecurityUtils.getCurrentUser().id(),
                                                pageable)));
        }

        @DeleteMapping("/groups/{groupId}/users/{userId}")
        public ResponseEntity<ApiResponse<Void>> removeMember(
                        @PathVariable UUID groupId,
                        @PathVariable UUID userId) {
                groupMemberService.removeMember(groupId, userId);
                return ResponseEntity.ok(ApiResponse.success("Group member removed successfully", null));
        }

        @DeleteMapping("/groups/{groupId}/me")
        public ResponseEntity<ApiResponse<Void>> leaveGroup(
                        @PathVariable UUID groupId) {
                groupMemberService.leaveGroup(groupId);
                return ResponseEntity.ok(
                                ApiResponse.success("Left group successfully", null));
        }
}
