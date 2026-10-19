package com.splitmate.dto.currency;

import java.time.LocalDateTime;

import com.splitmate.entity.Currency;

public record CurrencyResponse(
    String code,
    String name,
    String symbol,
    Integer decimalPlaces,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    public static CurrencyResponse toDto(Currency currency) {
        return new CurrencyResponse(
                currency.getCode(),
                currency.getName(),
                currency.getSymbol(),
                currency.getDecimalPlaces(),
                currency.getCreatedAt(),
                currency.getUpdatedAt()
        );
    }
}
