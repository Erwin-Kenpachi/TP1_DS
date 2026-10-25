package pt.ulisboa.lisboapedala.history_reader.business;

import pt.ulisboa.lisboapedala.history_reader.TripReceiptDetailsDTO;
import pt.ulisboa.lisboapedala.history_reader.UserTripHistoryDTO;
import pt.ulisboa.lisboapedala.tariff_billing.InvoiceDTO;
import pt.ulisboa.lisboapedala.tariff_billing.TariffBillingFacade;
import pt.ulisboa.lisboapedala.trip_lifecycle.TripDTO;
import pt.ulisboa.lisboapedala.trip_lifecycle.TripLifecycleFacade;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Servico interno de agregacao de historico de viagens e faturas.
 * Nao possui persistencia propria de dominio (ADR-002).
 * Consulta estritamente as Fachadas Publicas de trip_lifecycle e tariff_billing.
 */
public class HistoryAggregationService {

    private final TripLifecycleFacade tripLifecycleFacade;
    private final TariffBillingFacade tariffBillingFacade;

    public HistoryAggregationService(TripLifecycleFacade tripLifecycleFacade,
                                     TariffBillingFacade tariffBillingFacade) {
        this.tripLifecycleFacade = tripLifecycleFacade;
        this.tariffBillingFacade = tariffBillingFacade;
    }

    public List<UserTripHistoryDTO> getUserCompleteHistory(String userId) {
        List<TripDTO> trips = tripLifecycleFacade.getTripsByUser(userId);
        List<UserTripHistoryDTO> result = new ArrayList<>();

        for (TripDTO trip : trips) {
            Optional<InvoiceDTO> invoiceOpt = tariffBillingFacade.getInvoiceByTripId(trip.tripId());
            result.add(new UserTripHistoryDTO(trip, invoiceOpt.orElse(null)));
        }

        return result;
    }

    public Optional<TripReceiptDetailsDTO> getTripReceiptDetails(String tripId) {
        Optional<TripDTO> tripOpt = tripLifecycleFacade.getTripById(tripId);
        if (tripOpt.isEmpty()) {
            return Optional.empty();
        }
        Optional<InvoiceDTO> invoiceOpt = tariffBillingFacade.getInvoiceByTripId(tripId);
        return Optional.of(new TripReceiptDetailsDTO(tripOpt.get(), invoiceOpt.orElse(null)));
    }
}
