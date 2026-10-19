package com.splitmate.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.splitmate.entity.Currency;

public interface CurrencyRepository extends JpaRepository<Currency, String> {
    
    boolean existsByCode(String code);
}
