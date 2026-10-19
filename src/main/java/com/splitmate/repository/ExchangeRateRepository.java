package com.splitmate.repository;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.splitmate.entity.ExchangeRate;

public interface ExchangeRateRepository extends JpaRepository<ExchangeRate, Long> {

    // Find the most recent exchange rate for a given base currency, target
    // currency, and effective date
    /*
     * find Top By
     * BaseCurrency_Code
     * And
     * TargetCurrency_Code
     * And
     * EffectiveDate
     * LessThanEqual
     * 
     * OrderBy
     * EffectiveDate
     * Desc
     */
    Optional<ExchangeRate> findTopByBaseCurrency_CodeAndTargetCurrency_CodeAndEffectiveDateLessThanEqualOrderByEffectiveDateDesc(
            String baseCurrencyCode,
            String targetCurrencyCode,
            LocalDate effectiveDate);
}
