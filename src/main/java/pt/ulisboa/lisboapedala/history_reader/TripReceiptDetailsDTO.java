package pt.ulisboa.lisboapedala.history_reader;

import pt.ulisboa.lisboapedala.tariff_billing.InvoiceDTO;
import pt.ulisboa.lisboapedala.trip_lifecycle.TripDTO;

/**
 * Contrato público para os detalhes de recibo e viagem consultados por identificador de viagem.
 */
public record TripReceiptDetailsDTO(
        TripDTO trip,
        InvoiceDTO invoice
) {}
