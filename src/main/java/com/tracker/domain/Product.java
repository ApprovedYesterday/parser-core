package com.tracker.domain;

public record Product(Sku sku, String name, Money currentPrice) {}
