package pt.ulisboa.lisboapedala.tariff_billing;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pt.ulisboa.lisboapedala.tariff_billing.business.TariffCalculator;

import static org.assertj.core.api.Assertions.assertThat;

class TariffCalculatorTest {

    private final TariffCalculator calculator = new TariffCalculator();

    @Test
    @DisplayName("Assinante com viagem de 30 min (<= 45 min) deve ter custo de 0€")
    void subscriberWithinFreePeriodShouldBeFree() {
        TariffCalculator.CalculationResult result = calculator.calculate(true, 30);
        assertThat(result.totalAmountCents()).isEqualTo(0);
        assertThat(result.taxCents()).isEqualTo(0);
    }

    @Test
    @DisplayName("Assinante exatamente com 45 min deve ter custo de 0€")
    void subscriberExactly45MinShouldBeFree() {
        TariffCalculator.CalculationResult result = calculator.calculate(true, 45);
        assertThat(result.totalAmountCents()).isEqualTo(0);
    }

    @Test
    @DisplayName("Assinante com 46 min deve pagar 1 bloco adicional (50 cêntimos)")
    void subscriber46MinShouldPayOneBlock() {
        TariffCalculator.CalculationResult result = calculator.calculate(true, 46);
        assertThat(result.totalAmountCents()).isEqualTo(50);
        assertThat(result.taxCents()).isGreaterThan(0);
    }

    @Test
    @DisplayName("Assinante com 60 min deve pagar 1 bloco adicional (50 cêntimos)")
    void subscriber60MinShouldPayOneBlock() {
        TariffCalculator.CalculationResult result = calculator.calculate(true, 60);
        assertThat(result.totalAmountCents()).isEqualTo(50);
    }

    @Test
    @DisplayName("Assinante com 61 min deve pagar 2 blocos adicionais (100 cêntimos)")
    void subscriber61MinShouldPayTwoBlocks() {
        TariffCalculator.CalculationResult result = calculator.calculate(true, 61);
        assertThat(result.totalAmountCents()).isEqualTo(100);
    }

    @Test
    @DisplayName("Utilizador ocasional deve pagar taxa base (150 cêntimos) + blocos de 15 min")
    void occasionalUserShouldPayBasePlusBlocks() {
        // 20 min -> 150 base + 2 blocos de 50 = 250 cêntimos
        TariffCalculator.CalculationResult result = calculator.calculate(false, 20);
        assertThat(result.totalAmountCents()).isEqualTo(250);
    }
}
