package com.splitmate.service;

import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.splitmate.dto.group.GroupMemberResponse;
import com.splitmate.dto.group.JoinGroupRequest;
import com.splitmate.entity.ExpenseGroup;
import com.splitmate.entity.GroupMember;
import com.splitmate.entity.User;
import com.splitmate.exception.BusinessException;
import com.splitmate.repository.ExpenseGroupRepository;
import com.splitmate.repository.ExpenseShareRepository;
import com.splitmate.repository.GroupMemberRepository;
import com.splitmate.repository.SettlementRepository;
import com.splitmate.response.PageResponse;
import com.splitmate.security.SecurityUtils;


@Service
@RequiredArgsConstructor
public class GroupMemberService {

    private final GroupMemberRepository groupMemberRepository;
    private final ExpenseGroupRepository expenseGroupRepository;
    private final UserService userService;
    private final DataPermissionService dataPermissionService;

    private final ExpenseShareRepository expenseShareRepository;
private final SettlementRepository settlementRepository;

    @Transactional
    public GroupMemberResponse joinGroup(JoinGroupRequest request, UUID userId) {
        dataPermissionService.requireSelf(userId);
        ExpenseGroup group = expenseGroupRepository.findByInviteCode(request.inviteCode())
                .orElseThrow(() -> new BusinessException("Group not found"));
        User user = userService.getUserEntityById(userId);

        if (groupMemberRepository.existsByGroup_IdAndUser_Id(group.getId(), user.getId())) {
            throw new BusinessException("User already joined this group");
        }

        GroupMember member = GroupMember.builder()
                .group(group)
                .user(user)
                .role("MEMBER")
                .build();

        return GroupMemberResponse.toDto(groupMemberRepository.save(member));
    }

    @Transactional(readOnly = true)
    public PageResponse<GroupMemberResponse> getMembersByGroupId(UUID groupId, Pageable pageable) {
        dataPermissionService.requireGroupMember(groupId);
        Page<GroupMemberResponse> page = groupMemberRepository.findByGroup_Id(groupId, pageable)
                .map(GroupMemberResponse::toDto);
        return PageResponse.of(page.getContent(), page.getNumber(), page.getSize(), page.getTotalElements());
    }

    @Transactional(readOnly = true)
    public PageResponse<GroupMemberResponse> getMembershipsByUserId(UUID userId, Pageable pageable) {
        dataPermissionService.requireSelf(userId);
        Page<GroupMemberResponse> page = groupMemberRepository.findByUser_Id(userId, pageable)
                .map(GroupMemberResponse::toDto);
        return PageResponse.of(page.getContent(), page.getNumber(), page.getSize(), page.getTotalElements());
    }

    @Transactional
public void removeMember(UUID groupId, UUID userId) {
    dataPermissionService.requireGroupOwner(groupId);

    GroupMember member = groupMemberRepository
            .findByGroup_IdAndUser_Id(groupId, userId)
            .orElseThrow(() -> new BusinessException("Group member not found"));

    requireCanLeave(member);
    groupMemberRepository.delete(member);
}

@Transactional
public void leaveGroup(UUID groupId) {
    UUID userId = SecurityUtils.getCurrentUser().id();

    GroupMember member = groupMemberRepository
            .findByGroup_IdAndUser_Id(groupId, userId)
            .orElseThrow(() -> new BusinessException("You are not a member of this group"));

    requireCanLeave(member);
    groupMemberRepository.delete(member);
}

private void requireCanLeave(GroupMember member) {
    UUID groupId = member.getGroup().getId();
    UUID userId = member.getUser().getId();

    if (member.getGroup().getOwner().getId().equals(userId)) {
        throw new BusinessException("Transfer group ownership before leaving");
    }

    BigDecimal netAmount = expenseShareRepository
            .sumExpenseNetAmount(groupId, userId)
            .add(settlementRepository.sumSentAmount(groupId, userId))
            .subtract(settlementRepository.sumReceivedAmount(groupId, userId));

    if (netAmount.compareTo(BigDecimal.ZERO) != 0) {
        throw new BusinessException("Settle your group balance before leaving");
    }
}

    @Transactional(readOnly = true)
    public void validateUserInGroup(UUID groupId, UUID userId) {
        if (!groupMemberRepository.existsByGroup_IdAndUser_Id(groupId, userId)) {
            throw new BusinessException("User is not a member of this group");
        }
    }
}
