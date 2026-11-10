package com.example.parqueadero.service;

import com.example.parqueadero.data.TextFileRepository;
import com.example.parqueadero.model.BillingMode;
import com.example.parqueadero.model.MonthlyPass;
import com.example.parqueadero.model.ParkingSession;
import com.example.parqueadero.model.ParkingSpot;
import com.example.parqueadero.model.TariffConfig;
import com.example.parqueadero.model.VehicleType;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ParkingService {
    private final TextFileRepository repository;
    private final List<ParkingSpot> spots;
    private final List<ParkingSession> sessions;
    private final List<MonthlyPass> passes;
    private TariffConfig tariffs;

    public ParkingService(TextFileRepository repository) {
        this.repository = repository;
        this.spots = new ArrayList<>(repository.loadSpots());
        this.sessions = new ArrayList<>(repository.loadSessions());
        this.passes = new ArrayList<>(repository.loadPasses());
        this.tariffs = repository.loadTariffs();
        seedIfNeeded();
    }

    private void seedIfNeeded() {
        if (repository.isFirstRun() && spots.isEmpty()) {
            for (int i = 1; i <= 20; i++) {
                VehicleType type = i <= 10 ? VehicleType.CARRO : VehicleType.MOTO;
                int position = i;
                spots.add(new ParkingSpot(String.format(Locale.ROOT, "%s-%02d", type == VehicleType.CARRO ? "C" : "M",
                        type == VehicleType.CARRO ? i : i - 10), type, (position - 1) / 5 + 1, (position - 1) % 5 + 1));
            }
            repository.saveSpots(spots);
        }
        if (repository.isFirstRun() && passes.isEmpty()) {
            // Registro de ejemplo para mostrar el flujo de mensualidades. Se puede editar o borrar.
            passes.add(new MonthlyPass(newId(), "ABC123", "Cliente de ejemplo", VehicleType.CARRO,
                    LocalDate.now().minusDays(10), LocalDate.now().plusDays(20)));
            repository.savePasses(passes);
        }
        if (repository.loadTariffs().equals(TariffConfig.defaults())) repository.saveTariffs(tariffs);
    }

    private String newId() { return UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT); }
    private String normalizePlate(String plate) { return plate == null ? "" : plate.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]", ""); }

    public synchronized List<ParkingSpot> getSpots() {
        return spots.stream().sorted(Comparator.comparingInt(ParkingSpot::row).thenComparingInt(ParkingSpot::column)).toList();
    }
    public synchronized List<ParkingSession> getSessions() { return List.copyOf(sessions); }
    public synchronized List<MonthlyPass> getPasses() { return List.copyOf(passes); }
    public synchronized TariffConfig getTariffs() { return tariffs; }
    public synchronized List<ParkingSession> getActiveSessions() {
        return sessions.stream().filter(ParkingSession::isActive).sorted(Comparator.comparing(ParkingSession::entryTime)).toList();
    }
    public synchronized ParkingSession getActiveSessionBySpot(String spotId) {
        return sessions.stream().filter(ParkingSession::isActive).filter(s -> s.spotId().equals(spotId)).findFirst().orElse(null);
    }
    public synchronized ParkingSession getActiveSessionByPlate(String plate) {
        String normalized = normalizePlate(plate);
        return sessions.stream().filter(ParkingSession::isActive).filter(s -> s.plate().equals(normalized)).findFirst().orElse(null);
    }
    public synchronized boolean isSpotOccupied(String spotId) { return getActiveSessionBySpot(spotId) != null; }
    public synchronized long getFreeSpotCount() { return spots.stream().filter(s -> !isSpotOccupied(s.id())).count(); }
    public synchronized long getOccupiedSpotCount() { return getActiveSessions().size(); }

    public synchronized ParkingSession registerEntry(String plate, VehicleType type) {
        String normalized = normalizePlate(plate);
        if (!normalized.matches("[A-Z0-9]{5,8}")) throw new IllegalArgumentException("Ingresa una placa válida de 5 a 8 caracteres.");
        if (type == null) throw new IllegalArgumentException("Selecciona el tipo de vehículo.");
        if (getActiveSessionByPlate(normalized) != null) throw new IllegalArgumentException("Esta placa ya tiene un vehículo dentro del parqueadero.");
        ParkingSpot spot = spots.stream().filter(s -> s.type() == type && !isSpotOccupied(s.id())).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No hay puestos libres para ese tipo de vehículo."));
        boolean monthly = hasActivePass(normalized, LocalDate.now(), type);
        ParkingSession session = new ParkingSession(newId(), normalized, type, spot.id(), LocalDateTime.now(), null,
                BigDecimal.ZERO, monthly);
        sessions.add(session);
        repository.saveSessions(sessions);
        return session;
    }

    public synchronized ParkingSession registerExit(String sessionId) {
        int index = indexOfSession(sessionId);
        if (index < 0) throw new IllegalArgumentException("No se encontró el registro de entrada.");
        ParkingSession old = sessions.get(index);
        if (!old.isActive()) throw new IllegalArgumentException("Este vehículo ya registró su salida.");
        LocalDateTime exit = LocalDateTime.now();
        BigDecimal amount = old.monthlyPass() ? BigDecimal.ZERO : calculateFee(old.entryTime(), exit, old.type());
        ParkingSession closed = old.close(exit, amount);
        sessions.set(index, closed);
        repository.saveSessions(sessions);
        return closed;
    }

    private int indexOfSession(String id) {
        for (int i = 0; i < sessions.size(); i++) if (sessions.get(i).id().equals(id)) return i;
        return -1;
    }

    /** Calcula el cobro base por hora o el esquema de bonificación por minuto. */
    public synchronized BigDecimal calculateFee(LocalDateTime entry, LocalDateTime exit, VehicleType type) {
        if (exit.isBefore(entry)) throw new IllegalArgumentException("La salida no puede ser anterior a la entrada.");
        long totalSeconds = Duration.between(entry, exit).getSeconds();
        long totalMinutesRoundedUp = (totalSeconds + 59) / 60;
        if (tariffs.billingMode() == BillingMode.HOURLY) {
            long billableHours = (totalMinutesRoundedUp + 59) / 60;
            return tariffs.ratePerHour(type).multiply(BigDecimal.valueOf(billableHours))
                    .setScale(0, RoundingMode.HALF_UP);
        }
        long billableMinutes = Math.max(0, totalMinutesRoundedUp - tariffs.freeMinutes());
        Map<LocalDate, Long> minutesByDay = new LinkedHashMap<>();
        for (long minute = 0; minute < billableMinutes; minute++) {
            LocalDate day = entry.plusMinutes((long) tariffs.freeMinutes() + minute).toLocalDate();
            minutesByDay.merge(day, 1L, Long::sum);
        }
        BigDecimal rate = tariffs.ratePerMinute(type);
        BigDecimal cap = tariffs.cap(type);
        BigDecimal total = BigDecimal.ZERO;
        for (long minutes : minutesByDay.values()) {
            BigDecimal daily = rate.multiply(BigDecimal.valueOf(minutes)).min(cap);
            total = total.add(daily);
        }
        return total.setScale(0, RoundingMode.HALF_UP);
    }

    public synchronized boolean hasActivePass(String plate, LocalDate date, VehicleType type) {
        String normalized = normalizePlate(plate);
        return passes.stream().anyMatch(p -> p.plate().equals(normalized) && p.type() == type && p.isActiveOn(date));
    }

    public synchronized void saveSpot(ParkingSpot spot, String previousId) {
        boolean idExists = spots.stream().anyMatch(s -> s.id().equalsIgnoreCase(spot.id()) && !s.id().equals(previousId));
        if (idExists) throw new IllegalArgumentException("Ya existe un puesto con ese código.");
        boolean positionExists = spots.stream().anyMatch(s -> s.row() == spot.row() && s.column() == spot.column() && !s.id().equals(previousId));
        if (positionExists) throw new IllegalArgumentException("Ya hay un puesto en esa fila y columna.");
        if (previousId != null) {
            int index = -1;
            for (int i = 0; i < spots.size(); i++) if (spots.get(i).id().equals(previousId)) index = i;
            if (index < 0) throw new IllegalArgumentException("No se encontró el puesto.");
            if (isSpotOccupied(previousId)) throw new IllegalArgumentException("No puedes editar un puesto ocupado.");
            spots.set(index, spot);
        } else spots.add(spot);
        repository.saveSpots(spots);
    }

    public synchronized void deleteSpot(String id) {
        if (isSpotOccupied(id)) throw new IllegalArgumentException("No puedes eliminar un puesto ocupado.");
        if (!spots.removeIf(s -> s.id().equals(id))) throw new IllegalArgumentException("No se encontró el puesto.");
        repository.saveSpots(spots);
    }

    public synchronized void savePass(MonthlyPass pass, String previousId) {
        String normalized = normalizePlate(pass.plate());
        if (!normalized.matches("[A-Z0-9]{5,8}")) throw new IllegalArgumentException("La placa debe tener entre 5 y 8 caracteres.");
        if (pass.owner() == null || pass.owner().isBlank()) throw new IllegalArgumentException("El nombre del titular es obligatorio.");
        if (pass.endDate().isBefore(pass.startDate())) throw new IllegalArgumentException("La fecha final debe ser igual o posterior a la inicial.");
        boolean duplicate = passes.stream().anyMatch(p -> p.plate().equals(normalized) && !p.id().equals(previousId));
        if (duplicate) throw new IllegalArgumentException("Ya existe una mensualidad registrada para esa placa.");
        MonthlyPass normalizedPass = new MonthlyPass(pass.id(), normalized, pass.owner().trim(), pass.type(), pass.startDate(), pass.endDate());
        if (previousId != null) {
            int index = -1;
            for (int i = 0; i < passes.size(); i++) if (passes.get(i).id().equals(previousId)) index = i;
            if (index < 0) throw new IllegalArgumentException("No se encontró la mensualidad.");
            passes.set(index, normalizedPass);
        } else passes.add(normalizedPass);
        repository.savePasses(passes);
    }

    public synchronized void deletePass(String id) {
        if (!passes.removeIf(p -> p.id().equals(id))) throw new IllegalArgumentException("No se encontró la mensualidad.");
        repository.savePasses(passes);
    }

    public synchronized void saveTariffs(TariffConfig config) {
        if (config.carPerHour().signum() < 0 || config.motoPerHour().signum() < 0
                || config.carPerMinute().signum() < 0 || config.motoPerMinute().signum() < 0
                || config.carDailyCap().signum() < 0 || config.motoDailyCap().signum() < 0
                || config.freeMinutes() < 0 || config.freeMinutes() > 180) {
            throw new IllegalArgumentException("Las tarifas y topes no pueden ser negativos; los minutos gratis deben estar entre 0 y 180.");
        }
        tariffs = config;
        repository.saveTariffs(tariffs);
    }

    public synchronized List<ParkingSession> searchSessions(String plate, LocalDateTime from, LocalDateTime to) {
        String term = normalizePlate(plate);
        return sessions.stream()
                .filter(s -> term.isBlank() || s.plate().contains(term))
                .filter(s -> from == null || !s.entryTime().isBefore(from))
                .filter(s -> to == null || !s.entryTime().isAfter(to))
                .sorted(Comparator.comparing(ParkingSession::entryTime).reversed()).toList();
    }

    public synchronized BigDecimal revenueOn(LocalDate day) {
        return sessions.stream().filter(s -> !s.isActive() && s.exitTime().toLocalDate().equals(day))
                .map(ParkingSession::fee).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public synchronized BigDecimal revenueBetween(LocalDate start, LocalDate end) {
        return sessions.stream().filter(s -> !s.isActive())
                .filter(s -> !s.exitTime().toLocalDate().isBefore(start) && !s.exitTime().toLocalDate().isAfter(end))
                .map(ParkingSession::fee).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public synchronized Map<Integer, Double> averageOccupancyByHourLastWeek() {
        Map<Integer, Double> result = new LinkedHashMap<>();
        LocalDate today = LocalDate.now();
        for (int hour = 0; hour < 24; hour++) {
            int total = 0;
            for (int offset = 6; offset >= 0; offset--) {
                LocalDate day = today.minusDays(offset);
                LocalDateTime sample = LocalDateTime.of(day, LocalTime.of(hour, 30));
                total += (int) sessions.stream().filter(s -> !s.entryTime().isAfter(sample))
                        .filter(s -> s.exitTime() == null || s.exitTime().isAfter(sample)).count();
            }
            result.put(hour, total / 7.0);
        }
        return result;
    }

    public synchronized List<ParkingSession> getRecentSessions(int limit) {
        return sessions.stream().sorted(Comparator.comparing(ParkingSession::entryTime).reversed()).limit(limit).toList();
    }

    public synchronized long getMonthlyPassCountActive() {
        return passes.stream().filter(p -> p.isActiveOn(LocalDate.now())).count();
    }
}
