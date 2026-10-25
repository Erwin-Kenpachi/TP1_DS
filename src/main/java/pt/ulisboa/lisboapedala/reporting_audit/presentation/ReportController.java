package pt.ulisboa.lisboapedala.reporting_audit.presentation;

import pt.ulisboa.lisboapedala.reporting_audit.MonthlyMunicipalReportDTO;
import pt.ulisboa.lisboapedala.reporting_audit.ParishUsageReportDTO;
import pt.ulisboa.lisboapedala.reporting_audit.ReportingAuditFacade;
import pt.ulisboa.lisboapedala.reporting_audit.StationOpenDataDTO;

import java.util.Optional;

/**
 * Controlador da Camada de Apresentação para o módulo reporting_audit.
 * Ponto de entrada de pedidos de Gestores Municipais e portais de dados abertos.
 * Comunica estritamente através da Fachada Pública ReportingAuditFacade.
 */
public class ReportController {

    private final ReportingAuditFacade facade;

    public ReportController(ReportingAuditFacade facade) {
        this.facade = facade;
    }

    /**
     * Endpoint para consulta de utilização agregada por freguesia (GET /api/v1/admin/reports/parishes) - US-4
     */
    public ParishUsageReportDTO getParishUsageReport() {
        return facade.getParishUsageReport();
    }

    /**
     * Endpoint para gerar relatório mensal consolidado da Câmara (POST /api/v1/admin/reports/monthly)
     */
    public MonthlyMunicipalReportDTO generateMonthlyReport(int year, int month) {
        return facade.generateMonthlyReport(year, month);
    }

    /**
     * Endpoint para obter relatório arquivado por ano e mês (GET /api/v1/admin/reports/monthly/{year}/{month})
     */
    public Optional<MonthlyMunicipalReportDTO> getMonthlyReport(int year, int month) {
        return facade.getMonthlyReport(year, month);
    }

    /**
     * Endpoint público para publicação de dados abertos de ocupação (GET /api/v1/opendata/stations)
     */
    public StationOpenDataDTO getStationOpenData() {
        return facade.getStationOpenData();
    }
}
