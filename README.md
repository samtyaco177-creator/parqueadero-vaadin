# Parqueadero Central — Proyecto Final Vaadin 25

**Proyecto 15: Parqueadero** · Lógica de Programación · Java 21 · Spring Boot · Vaadin 25

Aplicación web para administrar puestos de parqueo, entradas y salidas de carros y motos, mensualidades, historial y reportes. La información se conserva en archivos de texto `.txt`; no utiliza base de datos.

## 1. Requisitos

- JDK 21.
- Maven 3.9 o superior.
- Node.js 24 o superior (Vaadin lo utiliza para preparar el frontend).
- Conexión a internet la primera vez para descargar dependencias.

## 2. Ejecutar en Visual Studio Code

1. Descomprime el proyecto y abre la carpeta `parqueadero-vaadin` en VS Code.
2. Verifica que VS Code esté usando JDK 21.
3. Abre una terminal en la carpeta donde está `pom.xml`.
4. Ejecuta:

   ```bash
   mvn spring-boot:run
   ```

5. Abre `http://localhost:8080` en el navegador.

También puedes importar el proyecto como proyecto Maven desde VS Code. Si quieres generar el ejecutable:

```bash
mvn clean package
java -jar target/parqueadero-1.0.0.jar
```

## 3. Funciones implementadas

- **Panel principal:** indicadores de puestos, ocupación, disponibilidad e ingresos del día. Plano interactivo con estado libre/ocupado. Cada puesto muestra detalles en un `Popover`.
- **Puestos:** CRUD de puestos con código, tipo, fila y columna. No permite editar ni eliminar puestos ocupados. Incluye búsqueda y filtro por tipo.
- **Entradas y salidas:** valida placas, evita que una placa esté dos veces dentro, asigna automáticamente el puesto libre compatible y registra la salida con recibo.
- **Mensualidades:** CRUD de placas, titular, tipo y vigencia. Los vehículos con mensualidad vigente no pagan al registrar la salida.
- **Historial:** búsqueda por placa, tipo de vehículo y rango de fecha/hora mediante `DateTimePicker`.
- **Reportes y tarifas:** ingresos del día y de los últimos 30 días, además de ocupación promedio por hora de los últimos siete días.
- **Diseño adaptable:** los formularios, el menú, los indicadores y el plano se ajustan a pantallas pequeñas.

## 4. Modalidad base y bonificación

La aplicación conserva las dos modalidades de cobro para cumplir el requisito principal y permitir activar la bonificación:

- **Por hora (modalidad inicial):** cada fracción de hora se cobra como una hora completa.
- **Por minuto (bonificación):** los primeros 15 minutos son gratis (editable), después se cobra por minuto, con un tope configurable por cada día calendario.
- Los cobros que cruzan la medianoche se separan por día y respetan el tope de cada fecha.
- El reporte muestra la ocupación promedio en cada hora, tomando una muestra a los 30 minutos de cada hora durante los últimos siete días.

Valores iniciales de demostración: tarifa base de **4.000 COP/hora para carro** y **2.000 COP/hora para moto**. En la modalidad bonificada se usan **100 COP/minuto y tope de 20.000 COP/día para carro**, y **50 COP/minuto y tope de 10.000 COP/día para moto**. No son tarifas oficiales. Se pueden modificar en **Reportes y tarifas**; cambia la modalidad en el selector para activar la bonificación por minuto.

## 5. Persistencia

Al iniciar, la aplicación crea automáticamente la carpeta `data/` y los siguientes archivos:

| Archivo | Contenido |
|---|---|
| `data/puestos.txt` | Código, tipo, fila y columna |
| `data/sesiones.txt` | Placa, tipo, puesto, entrada, salida, valor y mensualidad |
| `data/mensualidades.txt` | Placa, titular, tipo y vigencia |
| `data/tarifas.txt` | Modalidad, tarifas por hora y minuto, topes diarios y minutos gratis |

Los campos están separados por `|`. La lectura de cada línea está protegida con `try/catch`; una línea inválida se omite y se informa en la consola, sin detener toda la aplicación. En el primer arranque se crean 20 puestos de ejemplo y una mensualidad de demostración para `ABC123`.

> Haz una copia de la carpeta `data/` antes de borrar o reemplazar los archivos: allí quedan los datos reales registrados al usar la aplicación.

## 6. Estructura del proyecto

```text
src/main/java/com/example/parqueadero/
├── ParqueaderoApplication.java
├── data/
│   └── TextFileRepository.java
├── model/
│   ├── MonthlyPass.java
│   ├── ParkingSession.java
│   ├── ParkingSpot.java
│   ├── TariffConfig.java
│   └── VehicleType.java
├── service/
│   └── ParkingService.java
└── view/
    ├── DashboardView.java
    ├── HistoryView.java
    ├── MainLayout.java
    ├── MovementsView.java
    ├── MonthlyPassesView.java
    ├── ReportsView.java
    ├── SpotsView.java
    └── UiUtils.java
```

## 7. Componentes investigados del proyecto

- `DateTimePicker`: utilizado en Historial para consultar fecha y hora de inicio y fin.
- `Popover`: utilizado en el plano para consultar los datos del puesto y del vehículo.
- `Select`: utilizado para seleccionar el tipo de vehículo y filtrar listados.

## 8. Pruebas rápidas sugeridas

1. En **Puestos**, revisa los 20 puestos creados automáticamente.
2. En **Entradas y salidas**, registra un carro con placa `XYZ123`; comprueba que se le asigne un puesto tipo carro.
3. Registra otra vez `XYZ123`: el sistema debe impedir la entrada duplicada.
4. Registra una moto con placa `MOT123`; debe recibir un puesto tipo moto.
5. Registra la salida y revisa el recibo y el historial.
6. Prueba `ABC123`, que tiene una mensualidad de ejemplo vigente; el recibo debe indicar cobro cero.
7. Modifica una tarifa en **Reportes y tarifas**, reinicia la aplicación y verifica que el valor permanezca guardado.
8. Reduce temporalmente los minutos gratis o cambia una tarifa para probar el cálculo por minuto.

## 9. Antes de entregar

- Ejecuta `mvn clean package` y corrige cualquier error reportado por tu equipo.
- Prueba cada flujo y toma capturas de las vistas para agregarlas a este README o a `docs/`.
- Revisa el contenido de `data/` y conserva los datos de ejemplo.
- Crea un repositorio **público** en GitHub, sube el proyecto y pega el enlace en la tarea de Teams.
- Debes poder explicar cada clase y cada validación durante la sustentación.

## 10. Referencias técnicas

- Vaadin 25 — [Crear un proyecto](https://vaadin.com/docs/latest/getting-started/starters/start)
- [DateTimePicker](https://vaadin.com/docs/latest/components/date-time-picker)
- [Popover](https://vaadin.com/docs/latest/components/popover)
- [Select](https://vaadin.com/docs/latest/components/select)
- [Grid](https://vaadin.com/docs/latest/components/grid)
- [Confirm Dialog](https://vaadin.com/docs/latest/components/confirm-dialog)
