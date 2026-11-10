package com.example.parqueadero.view;

import com.example.parqueadero.model.ParkingSpot;
import com.example.parqueadero.model.VehicleType;
import com.example.parqueadero.service.ParkingService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@Route(value = "puestos", layout = MainLayout.class)
@PageTitle("Puestos | Parqueadero")
public class SpotsView extends VerticalLayout {
    private final ParkingService service;
    private final Grid<ParkingSpot> grid = new Grid<>(ParkingSpot.class, false);
    private final TextField search = new TextField("Buscar puesto");
    private final Select<VehicleType> typeFilter = new Select<>();

    public SpotsView(ParkingService service) {
        this.service = service;
        addClassName("page-view");
        setPadding(true);
        setSpacing(true);
        add(new H3("Administración de puestos"), new Paragraph("Crea, modifica o elimina puestos. No es posible modificar o borrar un puesto ocupado."));

        Button addButton = new Button("+ Nuevo puesto", e -> openForm(null));
        addButton.addClassName("primary-action");
        search.setPlaceholder("Código, por ejemplo C-01");
        search.setClearButtonVisible(true);
        search.setValueChangeMode(ValueChangeMode.EAGER);
        search.addValueChangeListener(e -> refreshGrid());
        typeFilter.setLabel("Tipo de puesto");
        typeFilter.setItems(VehicleType.values());
        typeFilter.setItemLabelGenerator(value -> value == null ? "Todos" : value.getLabel());
        typeFilter.setPlaceholder("Todos");
        typeFilter.setEmptySelectionAllowed(true);
        typeFilter.addValueChangeListener(e -> refreshGrid());
        HorizontalLayout toolbar = new HorizontalLayout(search, typeFilter, addButton);
        toolbar.setAlignItems(Alignment.END);
        toolbar.setWidthFull();
        toolbar.addClassName("toolbar-wrap");
        add(toolbar);

        grid.addColumn(ParkingSpot::id).setHeader("Código").setSortable(true);
        grid.addColumn(s -> s.type().getLabel()).setHeader("Tipo").setSortable(true);
        grid.addColumn(ParkingSpot::row).setHeader("Fila").setSortable(true);
        grid.addColumn(ParkingSpot::column).setHeader("Columna").setSortable(true);
        grid.addColumn(s -> service.isSpotOccupied(s.id()) ? "Ocupado" : "Libre").setHeader("Estado");
        grid.addComponentColumn(spot -> {
            Button edit = new Button("Editar", e -> openForm(spot));
            Button delete = new Button("Eliminar", e -> confirmDelete(spot));
            delete.addClassName("danger-action");
            return new HorizontalLayout(edit, delete);
        }).setHeader("Acciones");
        grid.setWidthFull();
        grid.setAllRowsVisible(true);
        add(grid);
        refreshGrid();
    }

    private void refreshGrid() {
        String term = search.getValue() == null ? "" : search.getValue().trim().toLowerCase();
        VehicleType type = typeFilter.getValue();
        grid.setItems(service.getSpots().stream()
                .filter(s -> term.isBlank() || s.id().toLowerCase().contains(term))
                .filter(s -> type == null || s.type() == type).toList());
    }

    private void openForm(ParkingSpot existing) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(existing == null ? "Crear puesto" : "Editar puesto");
        TextField id = new TextField("Código");
        id.setPlaceholder("Ej. C-21");
        id.setRequired(true);
        Select<VehicleType> type = new Select<>();
        type.setLabel("Tipo");
        type.setItems(VehicleType.values());
        type.setItemLabelGenerator(VehicleType::getLabel);
        type.setRequiredIndicatorVisible(true);
        IntegerField row = new IntegerField("Fila");
        IntegerField column = new IntegerField("Columna");
        row.setMin(1); row.setStepButtonsVisible(true);
        column.setMin(1); column.setStepButtonsVisible(true);
        if (existing != null) {
            id.setValue(existing.id()); id.setReadOnly(true);
            type.setValue(existing.type()); row.setValue(existing.row()); column.setValue(existing.column());
        }
        FormLayout form = new FormLayout(id, type, row, column);
        Button save = new Button("Guardar", e -> {
            if (id.isEmpty() || type.isEmpty() || row.isEmpty() || column.isEmpty()) {
                UiUtils.error("Completa todos los campos."); return;
            }
            try {
                ParkingSpot value = new ParkingSpot(id.getValue().trim().toUpperCase(), type.getValue(), row.getValue(), column.getValue());
                service.saveSpot(value, existing == null ? null : existing.id());
                dialog.close(); refreshGrid(); UiUtils.success("Puesto guardado correctamente.");
            } catch (Exception ex) { UiUtils.error(ex.getMessage()); }
        });
        Button cancel = new Button("Cancelar", e -> dialog.close());
        dialog.add(form);
        dialog.getFooter().add(cancel, save);
        dialog.open();
    }

    private void confirmDelete(ParkingSpot spot) {
        ConfirmDialog confirm = new ConfirmDialog();
        confirm.setHeader("Eliminar puesto " + spot.id() + "?");
        confirm.setText("Esta acción no se puede deshacer.");
        confirm.setCancelable(true);
        confirm.setConfirmText("Eliminar");
        confirm.setConfirmButtonTheme("error primary");
        confirm.addConfirmListener(e -> {
            try { service.deleteSpot(spot.id()); refreshGrid(); UiUtils.success("Puesto eliminado."); }
            catch (Exception ex) { UiUtils.error(ex.getMessage()); }
        });
        confirm.open();
    }
}
