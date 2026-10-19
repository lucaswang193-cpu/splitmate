package com.splitmate.dto.expense;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateExpenseRequest(

    @NotNull(message = "Expense id is required")
    UUID id,

    @NotNull(message = "Group id is required")
    UUID groupId,

    @NotNull(message = "Payer id is required")
    UUID payerId,

    @NotBlank(message = "Title is required")
    @Size(max = 200, message = "Title must be less than 200 characters")
    String title,

    Long categoryId,

    @NotBlank(message = "Currency is required")
    @Size(min = 3, max = 3, message = "Currency must be 3 characters")
    String currency,

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    @Digits(integer = 18, fraction = 2)
    BigDecimal amount,

    @NotNull(message = "Exchange rate is required")
    @DecimalMin(value = "0.000001", message = "Exchange rate must be greater than 0")
    @Digits(integer = 12, fraction = 6)
    BigDecimal exchangeRate,

    @NotNull(message = "Base amount is required")
    @DecimalMin(value = "0.01", message = "Base amount must be greater than 0")
    @Digits(integer = 18, fraction = 2)
    BigDecimal baseAmount,

    @NotNull(message = "Expense date and time are required")
    LocalDateTime expenseAt,

    String note,

    @NotEmpty(message = "Expense splits are required")
    List<@Valid ExpenseSplitRequest> splits
) {
}
