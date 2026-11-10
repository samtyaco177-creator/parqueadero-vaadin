package com.example.parqueadero.model;

import java.time.LocalDate;

public record MonthlyPass(String id, String plate, String owner, VehicleType type,
                          LocalDate startDate, LocalDate endDate) {
    public boolean isActiveOn(LocalDate date) {
        return !date.isBefore(startDate) && !date.isAfter(endDate);
    }
}
