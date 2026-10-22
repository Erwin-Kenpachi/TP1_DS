package pt.ulisboa.lisboapedala.tariff_billing;

/**
 * Contrato público para solicitação de faturação e cobrança de uma viagem.
 */
public record BillingRequestDTO(
        String tripId,
        String userId,
        boolean isSubscriber,
        long durationMinutes,
        String paymentMethod
) {}
