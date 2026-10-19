package com.splitmate.dto.exchange;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.splitmate.entity.ExchangeRate;

public record ExchangeRateResponse(
    Long id,
    String baseCurrency,
    String targetCurrency,
    BigDecimal rate,
    LocalDate effectiveDate,
    String source,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    public static ExchangeRateResponse toDto(ExchangeRate exchangeRate) {
        return new ExchangeRateResponse(
                exchangeRate.getId(),
                exchangeRate.getBaseCurrency().getCode(),
                exchangeRate.getTargetCurrency().getCode(),
                exchangeRate.getRate(),
                exchangeRate.getEffectiveDate(),
                exchangeRate.getSource(),
                exchangeRate.getCreatedAt(),
                exchangeRate.getUpdatedAt()
        );
    }
}
