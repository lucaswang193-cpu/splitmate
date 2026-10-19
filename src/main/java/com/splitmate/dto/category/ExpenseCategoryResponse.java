package com.splitmate.dto.category;

import java.time.LocalDateTime;

import com.splitmate.entity.ExpenseCategory;

public record ExpenseCategoryResponse(
    Long id,
    String name,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    public static ExpenseCategoryResponse toDto(ExpenseCategory category) {
        return new ExpenseCategoryResponse(
                category.getId(),
                category.getName(),
                category.getCreatedAt(),
                category.getUpdatedAt()
        );
    }
}
