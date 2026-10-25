package pt.ulisboa.lisboapedala.history_reader;

import pt.ulisboa.lisboapedala.tariff_billing.InvoiceDTO;
import pt.ulisboa.lisboapedala.trip_lifecycle.TripDTO;

/**
 * Contrato público representativo do histórico agregado de uma viagem e respetiva fatura.
 */
public record UserTripHistoryDTO(
        TripDTO trip,
        InvoiceDTO invoice
) {
    public boolean hasInvoice() {
        return invoice != null;
    }
}
