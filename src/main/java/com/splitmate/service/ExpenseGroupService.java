package com.splitmate.service;

import lombok.RequiredArgsConstructor;

import java.security.SecureRandom;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.splitmate.dto.group.CreateGroupRequest;
import com.splitmate.dto.group.GroupPreviewResponse;
import com.splitmate.dto.group.GroupResponse;
import com.splitmate.dto.group.TransferGroupOwnerRequest;
import com.splitmate.dto.group.UpdateGroupRequest;
import com.splitmate.entity.Currency;
import com.splitmate.entity.ExpenseGroup;
import com.splitmate.entity.GroupMember;
import com.splitmate.entity.User;
import com.splitmate.exception.BusinessException;
import com.splitmate.repository.ExpenseGroupRepository;
import com.splitmate.repository.GroupMemberRepository;


@Service
@RequiredArgsConstructor
public class ExpenseGroupService {

    private static final String INVITE_CODE_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int INVITE_CODE_LENGTH = 8;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final ExpenseGroupRepository expenseGroupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final UserService userService;
    private final CurrencyService currencyService;
    private final DataPermissionService dataPermissionService;
    @Transactional
    public GroupResponse createGroup(CreateGroupRequest request, UUID ownerId) {
        dataPermissionService.requireSelf(ownerId);
        User owner = userService.getUserEntityById(ownerId);
        Currency baseCurrency = currencyService.getCurrencyEntityByCode(request.baseCurrency());
        ExpenseGroup group = ExpenseGroup.builder()
                .name(request.name())
                .description(request.description())
                .owner(owner)
                .baseCurrency(baseCurrency)
                .inviteCode(generateUniqueInviteCode())
                .build();
        ExpenseGroup savedGroup = expenseGroupRepository.save(group);
        groupMemberRepository.save(GroupMember.builder()
                .group(savedGroup).user(owner).role("OWNER").build());
        return GroupResponse.toDto(savedGroup);
    }

    @Transactional(readOnly = true)
    public GroupResponse getGroupById(UUID groupId) {
        dataPermissionService.requireGroupMember(groupId);
        return GroupResponse.toDto(getGroupEntityById(groupId));
    }

    @Transactional(readOnly = true)
public GroupPreviewResponse getGroupByInviteCode(String inviteCode) {
    ExpenseGroup group = expenseGroupRepository.findByInviteCode(inviteCode)
            .orElseThrow(() -> new BusinessException("Group not found"));

    return GroupPreviewResponse.toDto(group);
}

    @Transactional
    public GroupResponse updateGroup(UUID groupId, UpdateGroupRequest request) {
        dataPermissionService.requireGroupOwner(groupId);
        ExpenseGroup group = getGroupEntityById(groupId);

        group.setName(request.name());
        group.setDescription(request.description());

        return GroupResponse.toDto(group);
    }

    @Transactional
    public GroupResponse transferOwner(UUID groupId, TransferGroupOwnerRequest request) {
        dataPermissionService.requireGroupOwner(groupId);
        ExpenseGroup group = getGroupEntityById(groupId);
        User newOwner = userService.getUserEntityById(request.newOwnerId());

        GroupMember currentOwnerMember = groupMemberRepository
                .findByGroup_IdAndUser_Id(groupId, group.getOwner().getId())
                .orElseThrow(() -> new BusinessException("Current owner membership not found"));
        GroupMember newOwnerMember = groupMemberRepository
                .findByGroup_IdAndUser_Id(groupId, newOwner.getId())
                .orElseThrow(() -> new BusinessException("New owner must be a group member"));

        currentOwnerMember.setRole("MEMBER");
        newOwnerMember.setRole("OWNER");
        group.setOwner(newOwner);

        return GroupResponse.toDto(group);
    }

    @Transactional(readOnly = true)
    public ExpenseGroup getGroupEntityById(UUID groupId) {
        return expenseGroupRepository.findById(groupId)
                .orElseThrow(() -> new BusinessException("Group not found"));
    }

    private String generateUniqueInviteCode() {
        String inviteCode;

        do {
            inviteCode = generateInviteCode();
        } while (expenseGroupRepository.existsByInviteCode(inviteCode));

        return inviteCode;
    }

    private String generateInviteCode() {
        StringBuilder builder = new StringBuilder(INVITE_CODE_LENGTH);

        for (int i = 0; i < INVITE_CODE_LENGTH; i++) {
            int index = RANDOM.nextInt(INVITE_CODE_CHARS.length());
            builder.append(INVITE_CODE_CHARS.charAt(index));
        }

        return builder.toString();
    }

}
