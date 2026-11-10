package com.example.parqueadero.view;

import com.example.parqueadero.model.ParkingSession;
import com.example.parqueadero.model.ParkingSpot;
import com.example.parqueadero.service.ParkingService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.popover.Popover;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Route(value = "", layout = MainLayout.class)
@PageTitle("Panel principal | Parqueadero")
public class DashboardView extends VerticalLayout {
    private final ParkingService service;

    public DashboardView(ParkingService service) {
        this.service = service;
        addClassName("page-view");
        setSizeFull();
        setPadding(true);
        setSpacing(true);

        add(new H3("Panel principal"), new Paragraph("Vista general de la ocupación y el estado del parqueadero."));
        HorizontalLayout stats = new HorizontalLayout(
                metric("Puestos totales", String.valueOf(service.getSpots().size()), "icon-blue"),
                metric("Ocupados", String.valueOf(service.getOccupiedSpotCount()), "icon-orange"),
                metric("Disponibles", String.valueOf(service.getFreeSpotCount()), "icon-green"),
                metric("Ingresos de hoy", UiUtils.money(service.revenueOn(LocalDate.now())), "icon-purple"));
        stats.addClassName("stats-row");
        stats.setWidthFull();
        add(stats);

        HorizontalLayout boardHeader = new HorizontalLayout();
        boardHeader.setWidthFull();
        boardHeader.setAlignItems(Alignment.CENTER);
        H3 boardTitle = new H3("Plano del parqueadero");
        boardHeader.add(boardTitle);
        boardHeader.expand(boardTitle);
        Span legend = new Span("● Libre     ● Ocupado");
        legend.addClassName("legend-text");
        boardHeader.add(legend);
        add(boardHeader);

        Div board = new Div();
        board.addClassName("parking-board");
        for (ParkingSpot spot : service.getSpots()) {
            ParkingSession active = service.getActiveSessionBySpot(spot.id());
            boolean occupied = active != null;
            Button button = new Button(spot.id() + "\n" + (occupied ? "Ocupado" : "Libre"));
            button.addClassName("spot-button");
            button.addClassName(occupied ? "spot-occupied" : "spot-free");
            button.setWidth("100%");
            Popover popover = new Popover();
            popover.setTarget(button);
            popover.add(new H4("Puesto " + spot.id()));
            popover.add(new Paragraph("Tipo: " + spot.type().getLabel()));
            if (occupied) {
                popover.add(new Paragraph("Placa: " + active.plate()));
                popover.add(new Paragraph("Entrada: " + active.entryTime().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))));
                popover.add(new Paragraph("Tiempo: " + active.getMinutesInside() + " minutos"));
            } else {
                popover.add(new Paragraph("Estado: Disponible"));
            }
            board.add(button, popover);
        }
        add(board);
        Paragraph hint = new Paragraph("Selecciona cualquier puesto para consultar sus detalles.");
        hint.addClassName("muted-text");
        add(hint);
    }

    private Div metric(String label, String value, String iconClass) {
        Div card = new Div();
        card.addClassName("metric-card");
        Span icon = new Span("●");
        icon.addClassName("metric-icon");
        icon.addClassName(iconClass);
        Span name = new Span(label);
        name.addClassName("metric-label");
        Span number = new Span(value);
        number.addClassName("metric-value");
        card.add(icon, name, number);
        return card;
    }
}
