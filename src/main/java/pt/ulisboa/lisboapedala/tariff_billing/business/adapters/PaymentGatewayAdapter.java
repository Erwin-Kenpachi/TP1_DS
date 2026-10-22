package pt.ulisboa.lisboapedala.tariff_billing.business.adapters;

/**
 * Adaptador simulado para processamento de pagamentos com MB WAY ou Cartão.
 */
public class PaymentGatewayAdapter {

    public record PaymentResult(boolean success, String transactionReference, String message) {}

    public PaymentResult processPayment(String userId, int amountCents, String paymentMethod) {
        if (amountCents <= 0) {
            // Pagamento de 0€ (ex: viagem gratuita de assinante dentro dos 45 min)
            return new PaymentResult(true, "TX-FREE-000", "Sem cobrança necessária");
        }
        // Simulação bem-sucedida
        String txRef = "TX-" + System.currentTimeMillis() + "-" + (int)(Math.random() * 1000);
        return new PaymentResult(true, txRef, "Pagamento aprovado via " + paymentMethod);
    }
}
