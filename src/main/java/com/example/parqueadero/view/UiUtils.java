package com.example.parqueadero.view;

import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.notification.Notification.Position;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

public final class UiUtils {
    private static final NumberFormat COP = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-CO"));
    private UiUtils() { }
    public static String money(BigDecimal value) { return COP.format(value == null ? BigDecimal.ZERO : value); }
    public static void success(String message) {
        Notification n = Notification.show(message, 3500, Position.BOTTOM_END);
        n.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }
    public static void error(String message) {
        Notification n = Notification.show(message, 5000, Position.BOTTOM_END);
        n.addThemeVariants(NotificationVariant.LUMO_ERROR);
    }
}
