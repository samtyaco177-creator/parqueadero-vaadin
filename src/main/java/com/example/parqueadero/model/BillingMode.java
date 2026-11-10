package com.example.parqueadero.model;

public enum BillingMode {
    HOURLY("Por hora"),
    BY_MINUTE("Por minuto · bonificación");

    private final String label;
    BillingMode(String label) { this.label = label; }
    public String getLabel() { return label; }
    @Override public String toString() { return label; }
}
