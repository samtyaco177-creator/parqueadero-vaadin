package com.example.parqueadero.model;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;

public record ParkingSession(
        String id,
        String plate,
        VehicleType type,
        String spotId,
        LocalDateTime entryTime,
        LocalDateTime exitTime,
        BigDecimal fee,
        boolean monthlyPass
) {
    public boolean isActive() { return exitTime == null; }

    public long getMinutesInside() {
        LocalDateTime end = isActive() ? LocalDateTime.now() : exitTime;
        return Math.max(0, Duration.between(entryTime, end).toMinutes());
    }

    public ParkingSession close(LocalDateTime exit, BigDecimal amount) {
        return new ParkingSession(id, plate, type, spotId, entryTime, exit, amount, monthlyPass);
    }
}
