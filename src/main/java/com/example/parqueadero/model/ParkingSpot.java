package com.example.parqueadero.model;

public record ParkingSpot(String id, VehicleType type, int row, int column) {
    public ParkingSpot {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("El código del puesto es obligatorio.");
        if (type == null) throw new IllegalArgumentException("El tipo de puesto es obligatorio.");
        if (row < 1 || column < 1) throw new IllegalArgumentException("La fila y la columna deben ser mayores que cero.");
    }
}
