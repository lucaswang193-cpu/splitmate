package com.splitmate.dto.group;

import java.time.LocalDateTime;
import java.util.UUID;

import com.splitmate.entity.GroupMember;

public record GroupMemberResponse(
    Long id,
    UUID groupId,
    String groupName,
    UUID userId,
    String userName,
    String userEmail,
    String role,
    LocalDateTime joinedAt,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    public static GroupMemberResponse toDto(GroupMember member) {
        return new GroupMemberResponse(
                member.getId(),
                member.getGroup().getId(),
                member.getGroup().getName(),
                member.getUser().getId(),
                member.getUser().getName(),
                member.getUser().getEmail(),
                member.getRole(),
                member.getJoinedAt(),
                member.getCreatedAt(),
                member.getUpdatedAt()
        );
    }
}
