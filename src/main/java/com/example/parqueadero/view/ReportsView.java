package com.example.parqueadero.view;

import com.example.parqueadero.model.BillingMode;
import com.example.parqueadero.model.TariffConfig;
import com.example.parqueadero.service.ParkingService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.IntStream;
import java.util.Map;

@Route(value = "reportes", layout = MainLayout.class)
@PageTitle("Reportes y tarifas | Parqueadero")
public class ReportsView extends VerticalLayout {
    private record DailyRevenue(LocalDate day, BigDecimal total) { }

    private final ParkingService service;

    public ReportsView(ParkingService service) {
        this.service = service;
        addClassName("page-view"); setPadding(true); setSpacing(true);
        add(new H3("Reportes y tarifas"), new Paragraph("Consulta los ingresos y la ocupación. Las tarifas se pueden ajustar sin modificar el código."));

        HorizontalLayout summary = new HorizontalLayout(
                metric("Ingresos de hoy", UiUtils.money(service.revenueOn(LocalDate.now()))),
                metric("Ingresos últimos 30 días", UiUtils.money(service.revenueBetween(LocalDate.now().minusDays(29), LocalDate.now()))),
                metric("Mensualidades vigentes", String.valueOf(service.getMonthlyPassCountActive())));
        summary.addClassName("stats-row"); summary.setWidthFull(); add(summary);

        add(new H4("Ingresos por día · últimos 7 días"));
        Grid<DailyRevenue> dailyGrid = new Grid<>(DailyRevenue.class, false);
        dailyGrid.addColumn(item -> item.day().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))).setHeader("Fecha");
        dailyGrid.addColumn(item -> UiUtils.money(item.total())).setHeader("Ingresos");
        List<DailyRevenue> daily = IntStream.rangeClosed(0, 6)
                .mapToObj(offset -> LocalDate.now().minusDays(offset))
                .map(day -> new DailyRevenue(day, service.revenueOn(day))).toList();
        dailyGrid.setItems(daily);
        dailyGrid.setAllRowsVisible(true);
        dailyGrid.setWidthFull();
        add(dailyGrid);

        add(new H4("Ocupación promedio por hora · últimos 7 días"));
        add(occupancyChart(service.averageOccupancyByHourLastWeek()));
        add(new Paragraph("Cada barra representa el promedio de puestos ocupados a las 30 minutos de cada hora, durante los últimos siete días."));

        add(new H4("Configuración de tarifas (bonificación)"));
        add(new Paragraph("Puedes conservar el cobro base por hora o activar la bonificación por minuto. Los valores se expresan en pesos colombianos."));
        add(tariffForm());
    }

    private Div metric(String label, String value) {
        Div card = new Div(); card.addClassName("metric-card");
        Span name = new Span(label); name.addClassName("metric-label");
        Span number = new Span(value); number.addClassName("metric-value");
        card.add(name, number); return card;
    }

    private Div occupancyChart(Map<Integer, Double> values) {
        Div chart = new Div(); chart.addClassName("occupancy-chart");
        double max = Math.max(1.0, values.values().stream().mapToDouble(Double::doubleValue).max().orElse(1.0));
        values.forEach((hour, average) -> {
            Div column = new Div(); column.addClassName("occupancy-column");
            Div barArea = new Div(); barArea.addClassName("occupancy-bar-area");
            Div bar = new Div(); bar.addClassName("occupancy-bar");
            double percentage = Math.max(3, (average / max) * 100.0);
            bar.getStyle().set("height", percentage + "%");
            bar.getElement().setAttribute("title", String.format("%.2f puestos promedio", average));
            barArea.add(bar);
            Span value = new Span(String.format("%.1f", average)); value.addClassName("occupancy-value");
            Span label = new Span(String.format("%02d h", hour)); label.addClassName("occupancy-label");
            column.add(value, barArea, label); chart.add(column);
        });
        return chart;
    }

    private VerticalLayout tariffForm() {
        TariffConfig config = service.getTariffs();
        Select<BillingMode> mode = new Select<>();
        mode.setLabel("Modalidad de cobro");
        mode.setItems(BillingMode.values());
        mode.setItemLabelGenerator(BillingMode::getLabel);
        mode.setValue(config.billingMode());

        NumberField carHour = number("Carro · valor por hora", config.carPerHour());
        NumberField motoHour = number("Moto · valor por hora", config.motoPerHour());
        NumberField carMinute = number("Carro · valor por minuto", config.carPerMinute());
        NumberField motoMinute = number("Moto · valor por minuto", config.motoPerMinute());
        NumberField carCap = number("Carro · tope por día (minutos)", config.carDailyCap());
        NumberField motoCap = number("Moto · tope por día (minutos)", config.motoDailyCap());
        IntegerField free = new IntegerField("Minutos gratis (modo por minuto)");
        free.setMin(0); free.setMax(180); free.setStepButtonsVisible(true); free.setValue(config.freeMinutes());

        FormLayout form = new FormLayout(mode, carHour, motoHour, carMinute, motoMinute, carCap, motoCap, free);
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1), new FormLayout.ResponsiveStep("700px", 2));
        Button save = new Button("Guardar tarifas", e -> {
            try {
                if (mode.isEmpty() || carHour.isEmpty() || motoHour.isEmpty() || carMinute.isEmpty()
                        || motoMinute.isEmpty() || carCap.isEmpty() || motoCap.isEmpty() || free.isEmpty()) {
                    UiUtils.error("Completa todos los valores de las tarifas."); return;
                }
                TariffConfig updated = new TariffConfig(mode.getValue(),
                        BigDecimal.valueOf(carHour.getValue()), BigDecimal.valueOf(motoHour.getValue()),
                        BigDecimal.valueOf(carMinute.getValue()), BigDecimal.valueOf(motoMinute.getValue()),
                        BigDecimal.valueOf(carCap.getValue()), BigDecimal.valueOf(motoCap.getValue()), free.getValue());
                service.saveTariffs(updated); UiUtils.success("Tarifas actualizadas correctamente.");
            } catch (Exception ex) { UiUtils.error(ex.getMessage()); }
        });
        save.addClassName("primary-action");
        VerticalLayout container = new VerticalLayout(form, save); container.addClassName("form-card");
        return container;
    }

    private NumberField number(String label, BigDecimal value) {
        NumberField field = new NumberField(label);
        field.setMin(0); field.setStep(50); field.setStepButtonsVisible(true);
        field.setValue(value.doubleValue());
        return field;
    }
}
