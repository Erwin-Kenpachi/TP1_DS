package pt.ulisboa.lisboapedala.tariff_billing;

/**
 * Contrato público representativo de uma fatura emitida.
 */
public record InvoiceDTO(
        String invoiceId,
        String tripId,
        String userId,
        int amountCents,
        int taxCents,
        String paymentMethod,
        String status,
        String atFiscalCode,
        String issuedAt
) {}
