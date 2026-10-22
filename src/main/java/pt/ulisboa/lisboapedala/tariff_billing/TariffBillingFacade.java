package pt.ulisboa.lisboapedala.tariff_billing;

import pt.ulisboa.lisboapedala.common.DatabaseManager;
import pt.ulisboa.lisboapedala.tariff_billing.business.BillingService;
import pt.ulisboa.lisboapedala.tariff_billing.business.TariffCalculator;
import pt.ulisboa.lisboapedala.tariff_billing.business.adapters.PaymentGatewayAdapter;
import pt.ulisboa.lisboapedala.tariff_billing.business.adapters.TaxAuthorityAdapter;
import pt.ulisboa.lisboapedala.tariff_billing.persistence.InvoiceRepository;

import java.util.List;
import java.util.Optional;

/**
 * Fachada pública do módulo tariff_billing.
 * Único ponto de contacto público permitido para outros módulos.
 * Módulo folha: não depende de nenhum outro módulo de domínio.
 */
public class TariffBillingFacade {

    private final BillingService service;

    public TariffBillingFacade(DatabaseManager databaseManager) {
        databaseManager.executeScriptFromClasspath("/pt/ulisboa/lisboapedala/tariff_billing/persistence/schema.sql");
        InvoiceRepository repository = new InvoiceRepository(databaseManager);
        TariffCalculator calculator = new TariffCalculator();
        PaymentGatewayAdapter paymentGateway = new PaymentGatewayAdapter();
        TaxAuthorityAdapter taxAuthority = new TaxAuthorityAdapter();
        this.service = new BillingService(repository, calculator, paymentGateway, taxAuthority);
    }

    public InvoiceDTO processTripBilling(BillingRequestDTO request) {
        return service.processTripBilling(request);
    }

    public Optional<InvoiceDTO> getInvoiceByTripId(String tripId) {
        return service.getInvoiceByTripId(tripId);
    }

    public Optional<InvoiceDTO> getInvoiceById(String invoiceId) {
        return service.getInvoiceById(invoiceId);
    }

    public List<InvoiceDTO> getUserInvoices(String userId) {
        return service.getUserInvoices(userId);
    }
}
