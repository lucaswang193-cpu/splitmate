package com.splitmate.service;

import lombok.RequiredArgsConstructor;

import java.util.UUID;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.splitmate.entity.Expense;
import com.splitmate.entity.GroupMember;
import com.splitmate.repository.GroupMemberRepository;
import com.splitmate.security.SecurityUtils;


@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class DataPermissionService {
private final GroupMemberRepository groupMemberRepository;
    // 個人資料只能由本人操作。
    public void requireSelf(UUID userId) {
        if (!SecurityUtils.getCurrentUser().id().equals(userId)) {
            throw new AccessDeniedException("Access denied");
        }
    }

    public void requireGroupMember(UUID groupId) {
        getCurrentMembership(groupId);
    }

    public void requireGroupOwner(UUID groupId) {
        GroupMember member = getCurrentMembership(groupId);

        if (!member.getGroup().getOwner().getId()
                .equals(member.getUser().getId())) {
            throw new AccessDeniedException("Access denied");
        }
    }

    // 使用資料庫中的原付款人判斷，不能使用請求中的新付款人。
    public void requireExpenseEditor(Expense expense) {
        GroupMember member = getCurrentMembership(
                expense.getGroup().getId()
        );
        UUID currentUserId = member.getUser().getId();

        boolean isOwner = expense.getGroup().getOwner()
                .getId().equals(currentUserId);
        boolean isPayer = expense.getPayer()
                .getId().equals(currentUserId);

        if (!isOwner && !isPayer) {
            throw new AccessDeniedException("Access denied");
        }
    }

    private GroupMember getCurrentMembership(UUID groupId) {
        UUID currentUserId = SecurityUtils.getCurrentUser().id();

        return groupMemberRepository
                .findByGroup_IdAndUser_Id(groupId, currentUserId)
                .orElseThrow(
                        () -> new AccessDeniedException("Access denied")
                );
    }
}
