package pt.ulisboa.lisboapedala.history_reader;

import pt.ulisboa.lisboapedala.history_reader.business.HistoryAggregationService;
import pt.ulisboa.lisboapedala.tariff_billing.TariffBillingFacade;
import pt.ulisboa.lisboapedala.trip_lifecycle.TripLifecycleFacade;

import java.util.List;
import java.util.Optional;

/**
 * Fachada pública do módulo history_reader.
 * Componente exclusivo de consulta (leitura) que agrega viagens e faturas (ADR-002, ADR-005).
 * Não possui persistência própria nem autoridade de escrita sobre viagens ou faturas.
 */
public class HistoryReaderFacade {

    private final HistoryAggregationService service;

    public HistoryReaderFacade(TripLifecycleFacade tripLifecycleFacade,
                               TariffBillingFacade tariffBillingFacade) {
        this.service = new HistoryAggregationService(tripLifecycleFacade, tariffBillingFacade);
    }

    /**
     * Obtém o histórico completo unificado de viagens e respetivas faturas para um determinado utilizador.
     */
    public List<UserTripHistoryDTO> getUserCompleteHistory(String userId) {
        return service.getUserCompleteHistory(userId);
    }

    /**
     * Obtém os detalhes de viagem e recibo fiscal para uma determinada viagem.
     */
    public Optional<TripReceiptDetailsDTO> getTripReceiptDetails(String tripId) {
        return service.getTripReceiptDetails(tripId);
    }
}
