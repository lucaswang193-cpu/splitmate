package com.splitmate.repository.projection;

import java.math.BigDecimal;

public interface CategoryExpenseSummary {

    Long getCategoryId();

    String getCategoryName();

    Long getExpenseCount();

    BigDecimal getTotalAmount();
}
