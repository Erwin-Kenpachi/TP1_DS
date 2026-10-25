package pt.ulisboa.lisboapedala.reporting_audit;

import pt.ulisboa.lisboapedala.common.DatabaseManager;
import pt.ulisboa.lisboapedala.reporting_audit.business.CarbonSavingsCalculator;
import pt.ulisboa.lisboapedala.reporting_audit.business.ParishUsageAggregator;
import pt.ulisboa.lisboapedala.reporting_audit.business.ReportGeneratorService;
import pt.ulisboa.lisboapedala.reporting_audit.persistence.ReportRepository;
import pt.ulisboa.lisboapedala.station_availability.StationAvailabilityFacade;
import pt.ulisboa.lisboapedala.trip_lifecycle.TripLifecycleFacade;

import java.util.Optional;

/**
 * Fachada pública do módulo reporting_audit.
 * Único ponto de contacto público permitido para Gestores Municipais e auditores externos.
 *
 * Em conformidade com o ADR-002, ADR-005 e ARCHITECTURE.md:
 * Módulo periférico (I = 1.0) dependente exclusivamente, em modo de leitura, de:
 * - TripLifecycleFacade
 * - StationAvailabilityFacade
 */
public class ReportingAuditFacade {

    private final ReportGeneratorService service;

    public ReportingAuditFacade(DatabaseManager databaseManager,
                                TripLifecycleFacade tripLifecycleFacade,
                                StationAvailabilityFacade stationAvailabilityFacade) {
        databaseManager.executeScriptFromClasspath("/pt/ulisboa/lisboapedala/reporting_audit/persistence/schema.sql");
        ReportRepository repository = new ReportRepository(databaseManager);
        CarbonSavingsCalculator carbonCalculator = new CarbonSavingsCalculator();
        ParishUsageAggregator parishAggregator = new ParishUsageAggregator(carbonCalculator);
        this.service = new ReportGeneratorService(
                tripLifecycleFacade,
                stationAvailabilityFacade,
                carbonCalculator,
                parishAggregator,
                repository
        );
    }

    /**
     * Gera o relatório consolidado de utilização por freguesia (US-4).
     */
    public ParishUsageReportDTO getParishUsageReport() {
        return service.generateParishUsageReport();
    }

    /**
     * Gera e persiste o relatório mensal municipal oficial (Requisito 7).
     */
    public MonthlyMunicipalReportDTO generateMonthlyReport(int year, int month) {
        return service.generateMonthlyReport(year, month);
    }

    /**
     * Consulta um relatório mensal previamente gerado e arquivado para auditoria.
     */
    public Optional<MonthlyMunicipalReportDTO> getMonthlyReport(int year, int month) {
        return service.getMonthlyReport(year, month);
    }

    /**
     * Publica métricas consolidadas e abertas de ocupação de estações (Open Data).
     */
    public StationOpenDataDTO getStationOpenData() {
        return service.getStationOpenData();
    }
}
