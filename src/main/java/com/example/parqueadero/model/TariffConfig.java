package com.example.parqueadero.model;

import java.math.BigDecimal;

/** Tarifas editables desde la vista de reportes. Valores en pesos colombianos. */
public record TariffConfig(
        BillingMode billingMode,
        BigDecimal carPerHour,
        BigDecimal motoPerHour,
        BigDecimal carPerMinute,
        BigDecimal motoPerMinute,
        BigDecimal carDailyCap,
        BigDecimal motoDailyCap,
        int freeMinutes
) {
    public static TariffConfig defaults() {
        return new TariffConfig(BillingMode.HOURLY,
                new BigDecimal("4000"), new BigDecimal("2000"),
                new BigDecimal("100"), new BigDecimal("50"),
                new BigDecimal("20000"), new BigDecimal("10000"), 15);
    }

    public BigDecimal ratePerMinute(VehicleType type) {
        return type == VehicleType.CARRO ? carPerMinute : motoPerMinute;
    }

    public BigDecimal ratePerHour(VehicleType type) {
        return type == VehicleType.CARRO ? carPerHour : motoPerHour;
    }

    public BigDecimal cap(VehicleType type) {
        return type == VehicleType.CARRO ? carDailyCap : motoDailyCap;
    }
}
