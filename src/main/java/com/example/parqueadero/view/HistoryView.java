package com.example.parqueadero.view;

import com.example.parqueadero.model.ParkingSession;
import com.example.parqueadero.model.VehicleType;
import com.example.parqueadero.service.ParkingService;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.datetimepicker.DateTimePicker;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.Duration;

@Route(value = "historial", layout = MainLayout.class)
@PageTitle("Historial | Parqueadero")
public class HistoryView extends VerticalLayout {
    private final ParkingService service;
    private final Grid<ParkingSession> grid = new Grid<>(ParkingSession.class, false);
    private final TextField plate = new TextField("Placa");
    private final DateTimePicker from = new DateTimePicker("Desde");
    private final DateTimePicker to = new DateTimePicker("Hasta");
    private final Select<VehicleType> type = new Select<>();
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public HistoryView(ParkingService service) {
        this.service = service;
        addClassName("page-view"); setPadding(true); setSpacing(true);
        add(new H3("Historial de movimientos"), new Paragraph("Consulta entradas y salidas por placa y por rango de fecha y hora."));
        plate.setPlaceholder("Buscar placa"); plate.setClearButtonVisible(true); plate.setValueChangeMode(ValueChangeMode.EAGER);
        type.setLabel("Tipo"); type.setItems(VehicleType.values()); type.setItemLabelGenerator(value -> value == null ? "Todos" : value.getLabel());
        type.setEmptySelectionAllowed(true); type.setPlaceholder("Todos");
        from.setStep(Duration.ofMinutes(15)); to.setStep(Duration.ofMinutes(15));
        plate.addValueChangeListener(e -> refreshGrid());
        from.addValueChangeListener(e -> refreshGrid()); to.addValueChangeListener(e -> refreshGrid());
        type.addValueChangeListener(e -> refreshGrid());
        HorizontalLayout filters = new HorizontalLayout(plate, type, from, to);
        filters.setWidthFull(); filters.addClassName("toolbar-wrap"); filters.setAlignItems(Alignment.END);
        add(filters);

        grid.addColumn(ParkingSession::plate).setHeader("Placa").setSortable(true);
        grid.addColumn(s -> s.type().getLabel()).setHeader("Tipo");
        grid.addColumn(ParkingSession::spotId).setHeader("Puesto");
        grid.addColumn(s -> s.entryTime().format(DATE)).setHeader("Entrada").setSortable(true);
        grid.addColumn(s -> s.exitTime() == null ? "En parqueadero" : s.exitTime().format(DATE)).setHeader("Salida");
        grid.addColumn(s -> s.exitTime() == null ? "—" : UiUtils.money(s.fee())).setHeader("Valor pagado");
        grid.addColumn(s -> s.monthlyPass() ? "Sí" : "No").setHeader("Mensualidad");
        grid.setWidthFull(); grid.setAllRowsVisible(true); add(grid); refreshGrid();
    }

    private void refreshGrid() {
        LocalDateTime start = from.getValue();
        LocalDateTime finish = to.getValue();
        if (start != null && finish != null && finish.isBefore(start)) {
            to.setInvalid(true); to.setErrorMessage("La fecha final debe ser posterior a la inicial.");
            grid.setItems(java.util.List.of()); return;
        }
        to.setInvalid(false);
        VehicleType selectedType = type.getValue();
        grid.setItems(service.searchSessions(plate.getValue(), start, finish).stream()
                .filter(s -> selectedType == null || s.type() == selectedType).toList());
    }
}
