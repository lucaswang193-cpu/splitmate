package com.splitmate.config;

import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.splitmate.entity.Currency;
import com.splitmate.entity.ExpenseCategory;
import com.splitmate.enums.DefaultCurrency;
import com.splitmate.enums.DefaultExpenseCategory;
import com.splitmate.repository.CurrencyRepository;
import com.splitmate.repository.ExpenseCategoryRepository;

@Configuration
public class DataSeedConfig {

    @Bean
    ApplicationRunner seedDefaultData(
            CurrencyRepository currencyRepository,
            ExpenseCategoryRepository expenseCategoryRepository
    ) {
        return args -> {
            for (DefaultCurrency currency : DefaultCurrency.values()) {
                if (!currencyRepository.existsByCode(currency.code())) {
                    Currency entity = Currency.builder()
                            .code(currency.code())
                            .name(currency.displayName())
                            .symbol(currency.symbol())
                            .decimalPlaces(currency.decimalPlaces())
                            .build();

                    currencyRepository.save(entity);
                }
            }

            for (DefaultExpenseCategory category : DefaultExpenseCategory.values()) {
                if (!expenseCategoryRepository.existsByName(category.displayName())) {
                    ExpenseCategory entity = ExpenseCategory.builder()
                            .name(category.displayName())
                            .build();

                    expenseCategoryRepository.save(entity);
                }
            }
        };
    }
}
