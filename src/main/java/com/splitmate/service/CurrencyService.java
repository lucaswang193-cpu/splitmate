package com.splitmate.service;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.splitmate.dto.currency.CurrencyResponse;
import com.splitmate.entity.Currency;
import com.splitmate.exception.BusinessException;
import com.splitmate.repository.CurrencyRepository;
import com.splitmate.response.PageResponse;


@Service
@RequiredArgsConstructor
public class CurrencyService {

    private final CurrencyRepository currencyRepository;
    @Transactional(readOnly = true)
    public PageResponse<CurrencyResponse> getAllCurrencies(Pageable pageable) {
        Page<CurrencyResponse> page = currencyRepository.findAll(pageable)
                .map(CurrencyResponse::toDto);
        return PageResponse.of(page.getContent(), page.getNumber(), page.getSize(), page.getTotalElements());
    }

    @Transactional(readOnly = true)
    public Currency getCurrencyEntityByCode(String code) {
        return currencyRepository.findById(code)
                .orElseThrow(() -> new BusinessException("Currency not found"));
    }
}
