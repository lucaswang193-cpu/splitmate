package com.splitmate.enums;

public enum DefaultCurrency {

    TWD("TWD", "New Taiwan Dollar", "NT$", 0),
    USD("USD", "US Dollar", "$", 2),
    JPY("JPY", "Japanese Yen", "¥", 0),
    KRW("KRW", "South Korean Won", "₩", 0),
    EUR("EUR", "Euro", "€", 2),
    CNY("CNY", "Chinese Yuan", "¥", 2),
    HKD("HKD", "Hong Kong Dollar", "HK$", 2),
    SGD("SGD", "Singapore Dollar", "S$", 2),
    IDR("IDR", "Indonesian Rupiah", "Rp", 0),
    THB("THB", "Thai Baht", "฿", 2),
    GBP("GBP", "British Pound", "£", 2),
    VND("VND", "Vietnamese Dong", "₫", 0),
    AUD("AUD", "Australian Dollar", "A$", 2),
    NZD("NZD", "New Zealand Dollar", "NZ$", 2);

    private final String code;
    private final String name;
    private final String symbol;
    private final Integer decimalPlaces;

    DefaultCurrency(String code, String name, String symbol, Integer decimalPlaces) {
        this.code = code;
        this.name = name;
        this.symbol = symbol;
        this.decimalPlaces = decimalPlaces;
    }

    public String code() {
        return code;
    }

    public String displayName() {
        return name;
    }

    public String symbol() {
        return symbol;
    }

    public Integer decimalPlaces() {
        return decimalPlaces;
    }
}
