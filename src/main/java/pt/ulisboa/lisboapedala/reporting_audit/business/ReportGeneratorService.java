package pt.ulisboa.lisboapedala.reporting_audit.business;

import pt.ulisboa.lisboapedala.reporting_audit.MonthlyMunicipalReportDTO;
import pt.ulisboa.lisboapedala.reporting_audit.ParishUsageReportDTO;
import pt.ulisboa.lisboapedala.reporting_audit.StationOpenDataDTO;
import pt.ulisboa.lisboapedala.reporting_audit.persistence.ReportRepository;
import pt.ulisboa.lisboapedala.station_availability.StationAvailabilityFacade;
import pt.ulisboa.lisboapedala.station_availability.StationOccupancyDTO;
import pt.ulisboa.lisboapedala.trip_lifecycle.TripDTO;
import pt.ulisboa.lisboapedala.trip_lifecycle.TripLifecycleFacade;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Serviço central de agregação analítica e geração de relatórios municipais.
 * Consome estritamente através das fachadas públicas permitidas (TripLifecycleFacade e StationAvailabilityFacade).
 */
public class ReportGeneratorService {

    private final TripLifecycleFacade tripLifecycleFacade;
    private final StationAvailabilityFacade stationAvailabilityFacade;
    private final CarbonSavingsCalculator carbonCalculator;
    private final ParishUsageAggregator parishAggregator;
    private final ReportRepository reportRepository;

    public ReportGeneratorService(TripLifecycleFacade tripLifecycleFacade,
                                  StationAvailabilityFacade stationAvailabilityFacade,
                                  CarbonSavingsCalculator carbonCalculator,
                                  ParishUsageAggregator parishAggregator,
                                  ReportRepository reportRepository) {
        this.tripLifecycleFacade = tripLifecycleFacade;
        this.stationAvailabilityFacade = stationAvailabilityFacade;
        this.carbonCalculator = carbonCalculator;
        this.parishAggregator = parishAggregator;
        this.reportRepository = reportRepository;
    }

    public ParishUsageReportDTO generateParishUsageReport() {
        // Consulta todas as viagens concluídas até ao momento presente
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
        List<StationOccupancyDTO> stations = stationAvailabilityFacade.getAllStations();
        List<StationOpenDataDTO.StationOpenDataRecord> records = new ArrayList<>();

        int totalBikes = 0;
        int totalDocks = 0;

        for (StationOccupancyDTO station : stations) {
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
                stations.size(),
                totalBikes,
                totalDocks,
                records
        );
    }

    private Map<String, String> buildStationParishMap() {
        Map<String, String> map = new HashMap<>();
        List<StationOccupancyDTO> stations = stationAvailabilityFacade.getAllStations();
        for (StationOccupancyDTO st : stations) {
            map.put(st.stationId(), st.parish());
        }
        return map;
    }
}
