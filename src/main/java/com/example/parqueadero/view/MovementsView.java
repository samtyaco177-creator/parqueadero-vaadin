package com.example.parqueadero.view;

import com.example.parqueadero.model.ParkingSession;
import com.example.parqueadero.model.VehicleType;
import com.example.parqueadero.service.ParkingService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.time.format.DateTimeFormatter;

@Route(value = "movimientos", layout = MainLayout.class)
@PageTitle("Entradas y salidas | Parqueadero")
public class MovementsView extends VerticalLayout {
    private final ParkingService service;
    private final Grid<ParkingSession> grid = new Grid<>(ParkingSession.class, false);
    private final TextField plateSearch = new TextField("Buscar placa");
    private final Select<VehicleType> typeFilter = new Select<>();
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public MovementsView(ParkingService service) {
        this.service = service;
        addClassName("page-view"); setPadding(true); setSpacing(true);
        add(new H3("Entradas y salidas"), new Paragraph("Registra el ingreso; el sistema asigna automáticamente el puesto compatible disponible."));
        add(entryForm());
        add(new H3("Vehículos dentro del parqueadero"));

        plateSearch.setPlaceholder("ABC123"); plateSearch.setClearButtonVisible(true);
        plateSearch.setValueChangeMode(ValueChangeMode.EAGER);
        plateSearch.addValueChangeListener(e -> refreshGrid());
        typeFilter.setLabel("Tipo de vehículo"); typeFilter.setItems(VehicleType.values());
        typeFilter.setItemLabelGenerator(value -> value == null ? "Todos" : value.getLabel()); typeFilter.setPlaceholder("Todos");
        typeFilter.setEmptySelectionAllowed(true); typeFilter.addValueChangeListener(e -> refreshGrid());
        HorizontalLayout filters = new HorizontalLayout(plateSearch, typeFilter);
        filters.addClassName("toolbar-wrap"); filters.setAlignItems(Alignment.END);
        add(filters);

        grid.addColumn(ParkingSession::plate).setHeader("Placa").setSortable(true);
        grid.addColumn(s -> s.type().getLabel()).setHeader("Tipo");
        grid.addColumn(ParkingSession::spotId).setHeader("Puesto");
        grid.addColumn(s -> s.entryTime().format(DATE)).setHeader("Fecha y hora de entrada");
        grid.addColumn(s -> s.monthlyPass() ? "Mensualidad (sin cobro)" : service.getTariffs().billingMode().getLabel()).setHeader("Modalidad");
        grid.addColumn(ParkingSession::getMinutesInside).setHeader("Minutos dentro");
        grid.addComponentColumn(session -> {
            Button exit = new Button("Registrar salida", e -> confirmExit(session));
            exit.addClassName("primary-action"); return exit;
        }).setHeader("Acción");
        grid.setWidthFull(); grid.setAllRowsVisible(true);
        add(grid);
        refreshGrid();
    }

    private VerticalLayout entryForm() {
        VerticalLayout card = new VerticalLayout(); card.addClassName("form-card");
        H4 title = new H4("Registrar entrada");
        TextField plate = new TextField("Placa"); plate.setPlaceholder("ABC123"); plate.setMaxLength(8);
        Select<VehicleType> type = new Select<>(); type.setLabel("Tipo de vehículo");
        type.setItems(VehicleType.values()); type.setItemLabelGenerator(VehicleType::getLabel);
        type.setValue(VehicleType.CARRO); type.setWidth("220px");
        Button save = new Button("Registrar entrada", e -> {
            try {
                ParkingSession session = service.registerEntry(plate.getValue(), type.getValue());
                plate.clear(); refreshGrid();
                UiUtils.success("Entrada registrada. Puesto asignado: " + session.spotId() + (session.monthlyPass() ? " · Mensualidad vigente" : ""));
            } catch (Exception ex) { UiUtils.error(ex.getMessage()); }
        });
        save.addClassName("primary-action");
        FormLayout form = new FormLayout(plate, type); form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1), new FormLayout.ResponsiveStep("600px", 2));
        card.add(title, form, save);
        return card;
    }

    private void refreshGrid() {
        String plate = plateSearch.getValue() == null ? "" : plateSearch.getValue().trim().toUpperCase();
        VehicleType type = typeFilter.getValue();
        grid.setItems(service.getActiveSessions().stream()
                .filter(s -> plate.isBlank() || s.plate().contains(plate))
                .filter(s -> type == null || s.type() == type).toList());
    }

    private void confirmExit(ParkingSession session) {
        ConfirmDialog confirm = new ConfirmDialog();
        confirm.setHeader("Registrar salida de " + session.plate());
        confirm.setText("El sistema calculará el valor y liberará el puesto " + session.spotId() + ". ¿Deseas continuar?");
        confirm.setCancelable(true); confirm.setConfirmText("Registrar salida");
        confirm.addConfirmListener(e -> {
            try {
                ParkingSession closed = service.registerExit(session.id());
                refreshGrid(); showReceipt(closed);
            } catch (Exception ex) { UiUtils.error(ex.getMessage()); }
        });
        confirm.open();
    }

    private void showReceipt(ParkingSession session) {
        Dialog receipt = new Dialog(); receipt.setHeaderTitle("Recibo de parqueadero");
        VerticalLayout content = new VerticalLayout(); content.addClassName("receipt");
        content.add(new H4("PARQUEADERO CENTRAL"), new Paragraph("Placa: " + session.plate()),
                new Paragraph("Tipo: " + session.type().getLabel()), new Paragraph("Puesto: " + session.spotId()),
                new Paragraph("Entrada: " + session.entryTime().format(DATE)),
                new Paragraph("Salida: " + session.exitTime().format(DATE)),
                new Paragraph("Tiempo: " + session.getMinutesInside() + " minutos"),
                new H3("Total: " + UiUtils.money(session.fee())),
                new Paragraph(session.monthlyPass() ? "Mensualidad vigente: no se genera cobro." : "Gracias por utilizar nuestro servicio."));
        Button close = new Button("Cerrar", e -> receipt.close());
        receipt.add(content); receipt.getFooter().add(close); receipt.open();
    }
}
