package com.splitmate.service;

import lombok.RequiredArgsConstructor;

import java.time.LocalDate;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.splitmate.dto.exchange.ExchangeRateResponse;
import com.splitmate.entity.ExchangeRate;
import com.splitmate.exception.BusinessException;
import com.splitmate.repository.ExchangeRateRepository;


@Service
@RequiredArgsConstructor
public class ExchangeRateService {

    private final ExchangeRateRepository exchangeRateRepository;
    @Transactional(readOnly = true)
    public ExchangeRateResponse getLatestRate(String baseCurrency, String targetCurrency, LocalDate effectiveDate) {
        return ExchangeRateResponse.toDto(getLatestRateEntity(baseCurrency, targetCurrency, effectiveDate));
    }

    @Transactional(readOnly = true)
    public ExchangeRate getLatestRateEntity(String baseCurrency, String targetCurrency, LocalDate effectiveDate) {
        return exchangeRateRepository
                .findTopByBaseCurrency_CodeAndTargetCurrency_CodeAndEffectiveDateLessThanEqualOrderByEffectiveDateDesc(
                        baseCurrency,
                        targetCurrency,
                        effectiveDate
                )
                .orElseThrow(() -> new BusinessException("Exchange rate not found"));
    }
}
