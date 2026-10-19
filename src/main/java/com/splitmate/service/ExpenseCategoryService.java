package com.splitmate.service;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.splitmate.dto.category.ExpenseCategoryResponse;
import com.splitmate.entity.ExpenseCategory;
import com.splitmate.exception.BusinessException;
import com.splitmate.repository.ExpenseCategoryRepository;
import com.splitmate.response.PageResponse;


@Service
@RequiredArgsConstructor
public class ExpenseCategoryService {

    private final ExpenseCategoryRepository expenseCategoryRepository;
    @Transactional(readOnly = true)
    public PageResponse<ExpenseCategoryResponse> getAllCategories(Pageable pageable) {
        Page<ExpenseCategoryResponse> page = expenseCategoryRepository.findAll(pageable)
                .map(ExpenseCategoryResponse::toDto);
        return PageResponse.of(page.getContent(), page.getNumber(), page.getSize(), page.getTotalElements());
    }

    @Transactional(readOnly = true)
    public ExpenseCategory getCategoryEntityById(Long categoryId) {
        return expenseCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new BusinessException("Expense category not found"));
    }
}
