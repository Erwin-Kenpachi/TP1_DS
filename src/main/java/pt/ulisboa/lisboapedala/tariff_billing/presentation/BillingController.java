package pt.ulisboa.lisboapedala.tariff_billing.presentation;

import pt.ulisboa.lisboapedala.tariff_billing.InvoiceDTO;
import pt.ulisboa.lisboapedala.tariff_billing.TariffBillingFacade;

import java.util.List;
import java.util.Optional;

/**
 * Controlador de Apresentação (REST/App) para o módulo tariff_billing.
 * Permite a consulta e download de faturas emitidas.
 * Comunica estritamente através da Fachada Pública TariffBillingFacade.
 */
public class BillingController {

    private final TariffBillingFacade facade;

    public BillingController(TariffBillingFacade facade) {
        this.facade = facade;
    }

    public Optional<InvoiceDTO> getInvoiceByTrip(String tripId) {
        return facade.getInvoiceByTripId(tripId);
    }

    public Optional<InvoiceDTO> getInvoiceById(String invoiceId) {
        return facade.getInvoiceById(invoiceId);
    }

    public List<InvoiceDTO> getUserInvoices(String userId) {
        return facade.getUserInvoices(userId);
    }
}
