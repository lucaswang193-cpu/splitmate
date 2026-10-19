package com.splitmate.service;

import lombok.RequiredArgsConstructor;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.splitmate.dto.expense.ExpenseShareResponse;
import com.splitmate.entity.Expense;
import com.splitmate.exception.BusinessException;
import com.splitmate.mapper.ExpenseMapper;
import com.splitmate.repository.ExpenseRepository;
import com.splitmate.repository.ExpenseShareRepository;
import com.splitmate.response.PageResponse;


@Service
@RequiredArgsConstructor
public class ExpenseShareService {

    private final ExpenseShareRepository expenseShareRepository;
    private final DataPermissionService dataPermissionService;
    private final ExpenseRepository expenseRepository;
    @Transactional(readOnly = true)
    public PageResponse<ExpenseShareResponse> getSharesByExpenseId(UUID expenseId, Pageable pageable) {
        Expense expense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new BusinessException("Expense not found"));
        // 先確認群組權限，再查詢分攤資料。
        dataPermissionService.requireGroupMember(expense.getGroup().getId());

        Page<ExpenseShareResponse> page = expenseShareRepository.findByExpense_Id(expenseId, pageable)
                .map(ExpenseMapper::toDto);
        return PageResponse.of(page.getContent(), page.getNumber(), page.getSize(), page.getTotalElements());
    }

    @Transactional(readOnly = true)
    public PageResponse<ExpenseShareResponse> getSharesByUserId(UUID userId, Pageable pageable) {
        
        dataPermissionService.requireSelf(userId);
        Page<ExpenseShareResponse> page = expenseShareRepository.findByUser_Id(userId, pageable)
                .map(ExpenseMapper::toDto);
        return PageResponse.of(page.getContent(), page.getNumber(), page.getSize(), page.getTotalElements());
    }
}
