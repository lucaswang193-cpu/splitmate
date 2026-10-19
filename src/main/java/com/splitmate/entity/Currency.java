package com.splitmate.entity;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "currency")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Currency {

    @Id
    @Column(length = 3)
    private String code;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(length = 8)
    private String symbol;

    @Column(name = "decimal_places", nullable = false)
    private Integer decimalPlaces;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder.Default
    @OneToMany(mappedBy = "baseCurrency")
    private List<ExpenseGroup> baseCurrencyGroups = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "currency")
    private List<Expense> expenses = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "currency")
    private List<Settlement> settlements = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "baseCurrency")
    private List<ExchangeRate> baseExchangeRates = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "targetCurrency")
    private List<ExchangeRate> targetExchangeRates = new ArrayList<>();
}
