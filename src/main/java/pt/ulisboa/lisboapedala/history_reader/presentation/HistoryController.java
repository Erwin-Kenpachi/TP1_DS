package pt.ulisboa.lisboapedala.history_reader.presentation;

import pt.ulisboa.lisboapedala.history_reader.HistoryReaderFacade;
import pt.ulisboa.lisboapedala.history_reader.TripReceiptDetailsDTO;
import pt.ulisboa.lisboapedala.history_reader.UserTripHistoryDTO;

import java.util.List;
import java.util.Optional;

/**
 * Controlador da Camada de Apresentação para o módulo history_reader.
 * Disponibiliza endpoints/métodos para a aplicação cliente consultar o histórico unificado do utilizador.
 */
public class HistoryController {

    private final HistoryReaderFacade facade;

    public HistoryController(HistoryReaderFacade facade) {
        this.facade = facade;
    }

    public List<UserTripHistoryDTO> getUserHistory(String userId) {
        return facade.getUserCompleteHistory(userId);
    }

    public Optional<TripReceiptDetailsDTO> getTripReceiptDetails(String tripId) {
        return facade.getTripReceiptDetails(tripId);
    }
}
