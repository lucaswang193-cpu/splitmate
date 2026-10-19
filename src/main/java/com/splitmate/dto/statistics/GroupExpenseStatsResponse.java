package com.splitmate.dto.statistics;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record GroupExpenseStatsResponse(
        UUID groupId,
        String baseCurrency,
        LocalDateTime from,
        LocalDateTime to,
        long expenseCount,
        BigDecimal totalAmount,
        List<CategoryExpenseStatResponse> categories) {

}
