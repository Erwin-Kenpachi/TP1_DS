package pt.ulisboa.lisboapedala.reporting_audit.business;

import pt.ulisboa.lisboapedala.reporting_audit.MonthlyMunicipalReportDTO;
import pt.ulisboa.lisboapedala.reporting_audit.ParishUsageReportDTO;
import pt.ulisboa.lisboapedala.reporting_audit.StationOpenDataDTO;
import pt.ulisboa.lisboapedala.reporting_audit.persistence.ReportRepository;
import pt.ulisboa.lisboapedala.trip_lifecycle.TripDTO;
import pt.ulisboa.lisboapedala.trip_lifecycle.TripLifecycleFacade;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Serviço central de agregação analítica e geração de relatórios municipais.
 * Consome estritamente através da fachada pública permitida (TripLifecycleFacade),
 * em total conformidade com a regra Allowed-to-Use (Ce = 1).
 */
public class ReportGeneratorService {

    public record MunicipalStationRecord(
            String stationId,
            String name,
            String parish,
            int totalDocks,
            int availableBikes,
            int freeDocks
    ) {}

    private static final Map<String, String> DEFAULT_PARISH_MAP = Map.of(
            "ST-01", "Misericordia",
            "ST-02", "Avenidas Novas",
            "ST-03", "Santa Maria Maior",
            "ST-04", "Belem",
            "ST-05", "Santo Antonio",
            "ST-06", "Arroios"
    );

    private final TripLifecycleFacade tripLifecycleFacade;
    private final CarbonSavingsCalculator carbonCalculator;
    private final ParishUsageAggregator parishAggregator;
    private final ReportRepository reportRepository;
    private final Map<String, MunicipalStationRecord> municipalStations = new ConcurrentHashMap<>();

    public ReportGeneratorService(TripLifecycleFacade tripLifecycleFacade,
                                  CarbonSavingsCalculator carbonCalculator,
                                  ParishUsageAggregator parishAggregator,
                                  ReportRepository reportRepository) {
        this.tripLifecycleFacade = tripLifecycleFacade;
        this.carbonCalculator = carbonCalculator;
        this.parishAggregator = parishAggregator;
        this.reportRepository = reportRepository;
    }

    public void registerStation(String stationId, String name, String parish, int totalDocks, int availableBikes, int freeDocks) {
        municipalStations.put(stationId, new MunicipalStationRecord(stationId, name, parish, totalDocks, availableBikes, freeDocks));
    }

    public ParishUsageReportDTO generateParishUsageReport() {
        LocalDateTime start = LocalDateTime.of(2000, 1, 1, 0, 0);
        LocalDateTime end = LocalDateTime.now().plusDays(1);
        List<TripDTO> trips = tripLifecycleFacade.getCompletedTripsBetween(start, end);

        Map<String, String> stationParishMap = buildStationParishMap();
        return parishAggregator.aggregate(trips, stationParishMap);
    }

    public MonthlyMunicipalReportDTO generateMonthlyReport(int year, int month) {
        YearMonth ym = YearMonth.of(year, month);
        LocalDateTime start = ym.atDay(1).atStartOfDay();
        LocalDateTime end = ym.atEndOfMonth().atTime(LocalTime.MAX);

        List<TripDTO> trips = tripLifecycleFacade.getCompletedTripsBetween(start, end);
        Map<String, String> stationParishMap = buildStationParishMap();

        ParishUsageReportDTO parishReport = parishAggregator.aggregate(trips, stationParishMap);

        long totalDurationMinutes = parishReport.totalDurationMinutes();
        double estimatedDistanceKm = carbonCalculator.estimateDistanceKm(totalDurationMinutes);
        double totalCo2SavedKg = carbonCalculator.calculateCo2SavedKg(estimatedDistanceKm);

        MunicipalReport report = new MunicipalReport(
                year,
                month,
                trips.size(),
                totalDurationMinutes,
                estimatedDistanceKm,
                totalCo2SavedKg,
                parishReport.parishStats()
        );

        MonthlyMunicipalReportDTO dto = new MonthlyMunicipalReportDTO(
                report.getReportId(),
                report.getYear(),
                report.getMonth(),
                report.getGeneratedAt(),
                report.getTotalTrips(),
                report.getTotalDurationMinutes(),
                report.getEstimatedDistanceKm(),
                report.getTotalCo2SavedKg(),
                report.getParishStats()
        );

        reportRepository.saveMonthlyReport(dto);
        return dto;
    }

    public Optional<MonthlyMunicipalReportDTO> getMonthlyReport(int year, int month) {
        return reportRepository.findByYearAndMonth(year, month);
    }

    public StationOpenDataDTO getStationOpenData() {
        List<StationOpenDataDTO.StationOpenDataRecord> records = new ArrayList<>();

        int totalBikes = 0;
        int totalDocks = 0;

        for (MunicipalStationRecord station : municipalStations.values()) {
            totalBikes += station.availableBikes();
            totalDocks += station.freeDocks();

            double rate = (station.totalDocks() > 0)
                    ? ((double) station.availableBikes() / station.totalDocks()) * 100.0
                    : 0.0;

            records.add(new StationOpenDataDTO.StationOpenDataRecord(
                    station.stationId(),
                    station.name(),
                    station.parish(),
                    station.totalDocks(),
                    station.availableBikes(),
                    station.freeDocks(),
                    Math.round(rate * 10.0) / 10.0
            ));
        }

        return new StationOpenDataDTO(
                Instant.now(),
                municipalStations.size(),
                totalBikes,
                totalDocks,
                records
        );
    }

    private Map<String, String> buildStationParishMap() {
        Map<String, String> map = new HashMap<>(DEFAULT_PARISH_MAP);
        for (MunicipalStationRecord st : municipalStations.values()) {
            map.put(st.stationId(), st.parish());
        }
        return map;
    }
}
