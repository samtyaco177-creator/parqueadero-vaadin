package com.example.parqueadero.view;

import com.example.parqueadero.model.MonthlyPass;
import com.example.parqueadero.model.VehicleType;
import com.example.parqueadero.service.ParkingService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;

@Route(value = "mensualidades", layout = MainLayout.class)
@PageTitle("Mensualidades | Parqueadero")
public class MonthlyPassesView extends VerticalLayout {
    private final ParkingService service;
    private final Grid<MonthlyPass> grid = new Grid<>(MonthlyPass.class, false);
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public MonthlyPassesView(ParkingService service) {
        this.service = service;
        addClassName("page-view"); setPadding(true); setSpacing(true);
        add(new H3("Mensualidades"), new Paragraph("Administra los vehículos que tienen acceso sin cobro mientras su mensualidad esté vigente."));
        Button add = new Button("+ Nueva mensualidad", e -> openForm(null)); add.addClassName("primary-action");
        HorizontalLayout actions = new HorizontalLayout(add); actions.setWidthFull(); actions.setJustifyContentMode(JustifyContentMode.END);
        grid.addColumn(MonthlyPass::plate).setHeader("Placa").setSortable(true);
        grid.addColumn(MonthlyPass::owner).setHeader("Titular").setSortable(true);
        grid.addColumn(p -> p.type().getLabel()).setHeader("Tipo");
        grid.addColumn(p -> p.startDate().format(DATE)).setHeader("Desde");
        grid.addColumn(p -> p.endDate().format(DATE)).setHeader("Hasta");
        grid.addColumn(p -> p.isActiveOn(LocalDate.now()) ? "Vigente" : (p.endDate().isBefore(LocalDate.now()) ? "Vencida" : "Programada"))
                .setHeader("Estado");
        grid.addComponentColumn(pass -> {
            Button edit = new Button("Editar", e -> openForm(pass));
            Button delete = new Button("Eliminar", e -> confirmDelete(pass)); delete.addClassName("danger-action");
            return new HorizontalLayout(edit, delete);
        }).setHeader("Acciones");
        grid.setWidthFull(); grid.setAllRowsVisible(true);
        add(actions, grid); refreshGrid();
    }

    private void refreshGrid() { grid.setItems(service.getPasses()); }

    private void openForm(MonthlyPass existing) {
        Dialog dialog = new Dialog(); dialog.setHeaderTitle(existing == null ? "Registrar mensualidad" : "Editar mensualidad");
        TextField plate = new TextField("Placa"); plate.setPlaceholder("ABC123"); plate.setMaxLength(8);
        TextField owner = new TextField("Nombre del titular");
        Select<VehicleType> type = new Select<>(); type.setLabel("Tipo de vehículo");
        type.setItems(VehicleType.values()); type.setItemLabelGenerator(VehicleType::getLabel);
        DatePicker start = new DatePicker("Fecha inicial");
        DatePicker end = new DatePicker("Fecha final");
        if (existing != null) {
            plate.setValue(existing.plate()); owner.setValue(existing.owner()); type.setValue(existing.type());
            start.setValue(existing.startDate()); end.setValue(existing.endDate());
        } else {
            start.setValue(LocalDate.now()); end.setValue(LocalDate.now().plusMonths(1)); type.setValue(VehicleType.CARRO);
        }
        FormLayout form = new FormLayout(plate, owner, type, start, end);
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1), new FormLayout.ResponsiveStep("600px", 2));
        Button save = new Button("Guardar", e -> {
            if (plate.isEmpty() || owner.isEmpty() || type.isEmpty() || start.isEmpty() || end.isEmpty()) {
                UiUtils.error("Completa todos los campos."); return;
            }
            try {
                String id = existing == null ? UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT) : existing.id();
                MonthlyPass pass = new MonthlyPass(id, plate.getValue().trim().toUpperCase(), owner.getValue().trim(), type.getValue(), start.getValue(), end.getValue());
                service.savePass(pass, existing == null ? null : existing.id());
                dialog.close(); refreshGrid(); UiUtils.success("Mensualidad guardada correctamente.");
            } catch (Exception ex) { UiUtils.error(ex.getMessage()); }
        });
        Button cancel = new Button("Cancelar", e -> dialog.close());
        dialog.add(form); dialog.getFooter().add(cancel, save); dialog.open();
    }

    private void confirmDelete(MonthlyPass pass) {
        ConfirmDialog confirm = new ConfirmDialog();
        confirm.setHeader("Eliminar mensualidad de " + pass.plate() + "?");
        confirm.setText("El vehículo dejará de tener el beneficio de mensualidad si el registro está vigente.");
        confirm.setCancelable(true); confirm.setConfirmText("Eliminar"); confirm.setConfirmButtonTheme("error primary");
        confirm.addConfirmListener(e -> {
            try { service.deletePass(pass.id()); refreshGrid(); UiUtils.success("Mensualidad eliminada."); }
            catch (Exception ex) { UiUtils.error(ex.getMessage()); }
        });
        confirm.open();
    }
}
