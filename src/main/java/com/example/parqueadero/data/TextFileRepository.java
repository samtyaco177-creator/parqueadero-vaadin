package com.example.parqueadero.data;

import com.example.parqueadero.model.BillingMode;
import com.example.parqueadero.model.MonthlyPass;
import com.example.parqueadero.model.ParkingSession;
import com.example.parqueadero.model.ParkingSpot;
import com.example.parqueadero.model.TariffConfig;
import com.example.parqueadero.model.VehicleType;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

@Component
public class TextFileRepository {
    private final Path folder = Path.of("data");
    private final boolean firstRun;
    private final Path spotsFile = folder.resolve("puestos.txt");
    private final Path sessionsFile = folder.resolve("sesiones.txt");
    private final Path passesFile = folder.resolve("mensualidades.txt");
    private final Path tariffsFile = folder.resolve("tarifas.txt");

    public TextFileRepository() {
        this.firstRun = Files.notExists(folder.resolve("puestos.txt"))
                && Files.notExists(folder.resolve("sesiones.txt"))
                && Files.notExists(folder.resolve("mensualidades.txt"))
                && Files.notExists(folder.resolve("tarifas.txt"));
        try {
            Files.createDirectories(folder);
            createIfMissing(spotsFile);
            createIfMissing(sessionsFile);
            createIfMissing(passesFile);
            createIfMissing(tariffsFile);
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo preparar la carpeta data.", e);
        }
    }

    public boolean isFirstRun() { return firstRun; }

    private void createIfMissing(Path file) throws IOException {
        if (Files.notExists(file)) Files.createFile(file);
    }

    private List<String[]> readRows(Path file) {
        List<String[]> rows = new ArrayList<>();
        try {
            for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                if (line.isBlank() || line.startsWith("#")) continue;
                rows.add(line.split("\\|", -1));
            }
        } catch (IOException e) {
            System.err.println("No se pudo leer " + file + ": " + e.getMessage());
        }
        return rows;
    }

    private <T> List<T> read(Path file, Function<String[], T> parser) {
        List<T> result = new ArrayList<>();
        for (String[] row : readRows(file)) {
            try { result.add(parser.apply(row)); }
            catch (Exception e) { System.err.println("Línea inválida en " + file + ": " + e.getMessage()); }
        }
        return result;
    }

    private String clean(String value) {
        return value == null ? "" : value.replace("|", " ").replace("\n", " ").trim();
    }

    private void write(Path file, List<String> lines) {
        try {
            Files.write(file, lines, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo guardar " + file + ".", e);
        }
    }

    public List<ParkingSpot> loadSpots() {
        return read(spotsFile, r -> new ParkingSpot(r[0], VehicleType.valueOf(r[1]),
                Integer.parseInt(r[2]), Integer.parseInt(r[3])));
    }

    public void saveSpots(List<ParkingSpot> values) {
        write(spotsFile, values.stream().map(p -> String.join("|", clean(p.id()), p.type().name(),
                Integer.toString(p.row()), Integer.toString(p.column()))).toList());
    }

    public List<ParkingSession> loadSessions() {
        return read(sessionsFile, r -> new ParkingSession(r[0], r[1], VehicleType.valueOf(r[2]), r[3],
                LocalDateTime.parse(r[4]), r[5].isBlank() ? null : LocalDateTime.parse(r[5]),
                new BigDecimal(r[6]), Boolean.parseBoolean(r[7])));
    }

    public void saveSessions(List<ParkingSession> values) {
        write(sessionsFile, values.stream().map(s -> String.join("|", clean(s.id()), clean(s.plate()),
                s.type().name(), clean(s.spotId()), s.entryTime().toString(),
                s.exitTime() == null ? "" : s.exitTime().toString(), s.fee().toPlainString(),
                Boolean.toString(s.monthlyPass()))).toList());
    }

    public List<MonthlyPass> loadPasses() {
        return read(passesFile, r -> new MonthlyPass(r[0], r[1], r[2], VehicleType.valueOf(r[3]),
                LocalDate.parse(r[4]), LocalDate.parse(r[5])));
    }

    public void savePasses(List<MonthlyPass> values) {
        write(passesFile, values.stream().map(p -> String.join("|", clean(p.id()), clean(p.plate()),
                clean(p.owner()), p.type().name(), p.startDate().toString(), p.endDate().toString())).toList());
    }

    public TariffConfig loadTariffs() {
        List<String[]> rows = readRows(tariffsFile);
        if (rows.isEmpty()) return TariffConfig.defaults();
        try {
            String[] r = rows.get(0);
            return new TariffConfig(BillingMode.valueOf(r[0]), new BigDecimal(r[1]), new BigDecimal(r[2]),
                    new BigDecimal(r[3]), new BigDecimal(r[4]), new BigDecimal(r[5]),
                    new BigDecimal(r[6]), Integer.parseInt(r[7]));
        } catch (Exception e) {
            System.err.println("Tarifas inválidas; se usan valores predeterminados: " + e.getMessage());
            return TariffConfig.defaults();
        }
    }

    public void saveTariffs(TariffConfig t) {
        write(tariffsFile, List.of(String.join("|", t.billingMode().name(), t.carPerHour().toPlainString(),
                t.motoPerHour().toPlainString(), t.carPerMinute().toPlainString(), t.motoPerMinute().toPlainString(),
                t.carDailyCap().toPlainString(), t.motoDailyCap().toPlainString(), Integer.toString(t.freeMinutes()))));
    }
}
