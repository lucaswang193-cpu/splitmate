package com.splitmate.dto.group;

import com.splitmate.entity.ExpenseGroup;

public record GroupPreviewResponse(
    String name,
        String baseCurrency
) {

    public static GroupPreviewResponse toDto(ExpenseGroup group) {
        return new GroupPreviewResponse(
                group.getName(),
                group.getBaseCurrency().getCode()
        );
    }
}
