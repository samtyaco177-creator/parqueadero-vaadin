package com.example.parqueadero.view;

import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.RouterLink;
import com.example.parqueadero.view.DashboardView;
import com.example.parqueadero.view.SpotsView;
import com.example.parqueadero.view.MovementsView;
import com.example.parqueadero.view.MonthlyPassesView;
import com.example.parqueadero.view.HistoryView;
import com.example.parqueadero.view.ReportsView;

public class MainLayout extends AppLayout {
    public MainLayout() {
        DrawerToggle toggle = new DrawerToggle();
        H2 title = new H2("Parqueadero Central");
        title.addClassName("app-title");
        addToNavbar(toggle, title);

        VerticalLayout menu = new VerticalLayout();
        menu.addClassName("navigation-menu");
        menu.setPadding(true);
        menu.setSpacing(false);
        Span section = new Span("MENÚ PRINCIPAL");
        section.addClassName("nav-section");
        menu.add(section,
                nav("Panel principal", DashboardView.class),
                nav("Puestos", SpotsView.class),
                nav("Entradas y salidas", MovementsView.class),
                nav("Mensualidades", MonthlyPassesView.class),
                nav("Historial", HistoryView.class),
                nav("Reportes y tarifas", ReportsView.class));
        addToDrawer(menu);
    }

    private RouterLink nav(String label, Class<? extends com.vaadin.flow.component.Component> view) {
        RouterLink link = new RouterLink(label, view);
        link.addClassName("nav-link");
        return link;
    }
}
