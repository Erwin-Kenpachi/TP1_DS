package pt.ulisboa.lisboapedala.tariff_billing.business.adapters;

/**
 * Adaptador simulado para comunicação com o webservice da Autoridade Tributária (AT).
 */
public class TaxAuthorityAdapter {

    public String registerInvoice(String invoiceId, int amountCents, int taxCents) {
        // Gera um código de validação fiscal da AT simulado (ex: AT-2026-XXXX)
        long hash = Math.abs((invoiceId + amountCents).hashCode());
        return "AT-2026-" + String.format("%08d", hash % 100000000L);
    }
}
