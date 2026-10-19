package com.splitmate.controller;

import lombok.RequiredArgsConstructor;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.splitmate.dto.exchange.ExchangeRateResponse;
import com.splitmate.response.ApiResponse;
import com.splitmate.service.ExchangeRateService;


@RestController
@RequestMapping("/api/exchange-rates")
@RequiredArgsConstructor
public class ExchangeRateController {

    private final ExchangeRateService exchangeRateService;
    @GetMapping("/latest")
    public ResponseEntity<ApiResponse<ExchangeRateResponse>> getLatestRate(
            @RequestParam String baseCurrency,
            @RequestParam String targetCurrency,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate effectiveDate
    ) {
        ExchangeRateResponse response = exchangeRateService.getLatestRate(baseCurrency, targetCurrency, effectiveDate);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
