package pt.ulisboa.lisboapedala.tariff_billing.business;

/**
 * Calculador de tarifas do sistema Lisboa Pedala.
 * Regras:
 * - Assinantes (passe anual): 45 min gratuitos. Tempo excedente cobrado a 50 cêntimos por cada bloco de 15 min.
 * - Utilizadores Ocasionais: Taxa base de desbloqueio de 150 cêntimos + 50 cêntimos por cada bloco de 15 min.
 */
public class TariffCalculator {

    public static final int SUBSCRIBER_FREE_MINUTES = 45;
    public static final int BLOCK_MINUTES = 15;
    public static final int SUBSCRIBER_BLOCK_PRICE_CENTS = 50; // 0.50€ por bloco de 15 min
    public static final int OCCASIONAL_BASE_PRICE_CENTS = 150;  // 1.50€ taxa de desbloqueio
    public static final int OCCASIONAL_BLOCK_PRICE_CENTS = 50; // 0.50€ por bloco de 15 min

    public record CalculationResult(int totalAmountCents, int taxCents) {}

    public CalculationResult calculate(boolean isSubscriber, long durationMinutes) {
        int totalCents;
        if (isSubscriber) {
            if (durationMinutes <= SUBSCRIBER_FREE_MINUTES) {
                totalCents = 0;
            } else {
                long extraMinutes = durationMinutes - SUBSCRIBER_FREE_MINUTES;
                long blocks = (extraMinutes + BLOCK_MINUTES - 1) / BLOCK_MINUTES; // Arredondamento para cima
                totalCents = (int) (blocks * SUBSCRIBER_BLOCK_PRICE_CENTS);
            }
        } else {
            long blocks = durationMinutes == 0 ? 1 : ((durationMinutes + BLOCK_MINUTES - 1) / BLOCK_MINUTES);
            totalCents = OCCASIONAL_BASE_PRICE_CENTS + (int) (blocks * OCCASIONAL_BLOCK_PRICE_CENTS);
        }

        // IVA a 23% incluído no total (fórmula fiscal: total * 23 / 123)
        int taxCents = (int) Math.round(totalCents * 23.0 / 123.0);
        return new CalculationResult(totalCents, taxCents);
    }
}
