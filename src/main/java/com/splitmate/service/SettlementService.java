package com.splitmate.service;

import lombok.RequiredArgsConstructor;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.splitmate.dto.settlement.CreateSettlementRequest;
import com.splitmate.dto.settlement.SettlementResponse;
import com.splitmate.entity.ExpenseGroup;
import com.splitmate.entity.Settlement;
import com.splitmate.entity.User;
import com.splitmate.exception.BusinessException;
import com.splitmate.repository.SettlementRepository;
import com.splitmate.response.PageResponse;


@Service
@RequiredArgsConstructor
public class SettlementService {

        private final SettlementRepository settlementRepository;
        private final ExpenseGroupService expenseGroupService;
        private final UserService userService;
        private final GroupMemberService groupMemberService;
        private final DataPermissionService dataPermissionService;
        @Transactional
        public SettlementResponse createSettlement(CreateSettlementRequest request) {
                dataPermissionService.requireGroupMember(request.groupId());

                if (request.fromUserId().equals(request.toUserId())) {
                        throw new BusinessException("From user and to user must be different");
                }

                ExpenseGroup group = expenseGroupService.getGroupEntityById(request.groupId());

                // 結算沒有匯率欄位，因此先統一使用群組基本幣別
                if (!group.getBaseCurrency().getCode().equals(request.currency())) {
                        throw new BusinessException(
                                        "Settlement currency must match group base currency");
                }

                User fromUser = userService.getUserEntityById(request.fromUserId());
                User toUser = userService.getUserEntityById(request.toUserId());

                groupMemberService.validateUserInGroup(group.getId(), fromUser.getId());
                groupMemberService.validateUserInGroup(group.getId(), toUser.getId());

                Settlement settlement = Settlement.builder()
                                .group(group)
                                .fromUser(fromUser)
                                .toUser(toUser)
                                .amount(request.amount())
                                .currency(group.getBaseCurrency())
                                .build();

                return SettlementResponse.toDto(settlementRepository.save(settlement));
        }

        @Transactional(readOnly = true)
        public PageResponse<SettlementResponse> getSettlementsByGroupId(
                        UUID groupId,
                        Pageable pageable) {
                dataPermissionService.requireGroupMember(groupId);

                Page<SettlementResponse> page = settlementRepository.findByGroup_Id(groupId, pageable)
                                .map(SettlementResponse::toDto);

                return PageResponse.of(
                                page.getContent(),
                                page.getNumber(),
                                page.getSize(),
                                page.getTotalElements());
        }

        @Transactional(readOnly = true)
        public PageResponse<SettlementResponse> getSentSettlements(
                        UUID userId,
                        Pageable pageable) {
                dataPermissionService.requireSelf(userId);

                Page<SettlementResponse> page = settlementRepository.findByFromUser_Id(userId, pageable)
                                .map(SettlementResponse::toDto);

                return PageResponse.of(
                                page.getContent(),
                                page.getNumber(),
                                page.getSize(),
                                page.getTotalElements());
        }

        @Transactional(readOnly = true)
        public PageResponse<SettlementResponse> getReceivedSettlements(
                        UUID userId,
                        Pageable pageable) {
                dataPermissionService.requireSelf(userId);

                Page<SettlementResponse> page = settlementRepository.findByToUser_Id(userId, pageable)
                                .map(SettlementResponse::toDto);

                return PageResponse.of(
                                page.getContent(),
                                page.getNumber(),
                                page.getSize(),
                                page.getTotalElements());
        }

}
