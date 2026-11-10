package com.example.parqueadero.model;

public enum VehicleType {
    CARRO("Carro"),
    MOTO("Moto");

    private final String label;

    VehicleType(String label) { this.label = label; }
    public String getLabel() { return label; }
    @Override public String toString() { return label; }
}
