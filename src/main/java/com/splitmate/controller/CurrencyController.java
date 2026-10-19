package com.splitmate.controller;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.splitmate.dto.currency.CurrencyResponse;
import com.splitmate.response.ApiResponse;
import com.splitmate.response.PageResponse;
import com.splitmate.service.CurrencyService;


@RestController
@RequestMapping("/api/currencies")
@RequiredArgsConstructor
public class CurrencyController {

    private final CurrencyService currencyService;
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<CurrencyResponse>>> getAllCurrencies(Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(currencyService.getAllCurrencies(pageable)));
    }
}
