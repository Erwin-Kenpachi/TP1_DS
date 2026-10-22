package pt.ulisboa.lisboapedala.tariff_billing.business;

import pt.ulisboa.lisboapedala.tariff_billing.BillingRequestDTO;
import pt.ulisboa.lisboapedala.tariff_billing.InvoiceDTO;
import pt.ulisboa.lisboapedala.tariff_billing.business.adapters.PaymentGatewayAdapter;
import pt.ulisboa.lisboapedala.tariff_billing.business.adapters.TaxAuthorityAdapter;
import pt.ulisboa.lisboapedala.tariff_billing.persistence.InvoiceRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class BillingService {

    private final InvoiceRepository repository;
    private final TariffCalculator calculator;
    private final PaymentGatewayAdapter paymentGateway;
    private final TaxAuthorityAdapter taxAuthority;

    public BillingService(InvoiceRepository repository,
                          TariffCalculator calculator,
                          PaymentGatewayAdapter paymentGateway,
                          TaxAuthorityAdapter taxAuthority) {
        this.repository = repository;
        this.calculator = calculator;
        this.paymentGateway = paymentGateway;
        this.taxAuthority = taxAuthority;
    }

    public synchronized InvoiceDTO processTripBilling(BillingRequestDTO request) {
        // Garantia de Idempotência: Se já existir fatura para esta viagem, devolve sem repetir cobrança
        Optional<InvoiceDTO> existing = repository.findByTripId(request.tripId());
        if (existing.isPresent()) {
            return existing.get();
        }

        // 1. Cálculo da tarifa
        TariffCalculator.CalculationResult tariff = calculator.calculate(request.isSubscriber(), request.durationMinutes());

        // 2. Cobrança simulada
        PaymentGatewayAdapter.PaymentResult paymentResult = paymentGateway.processPayment(
                request.userId(),
                tariff.totalAmountCents(),
                request.paymentMethod()
        );

        if (!paymentResult.success()) {
            throw new IllegalStateException("Falha no pagamento: " + paymentResult.message());
        }

        String invoiceId = "INV-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String issuedAt = Instant.now().toString();

        // 3. Registo na Autoridade Tributária
        String atFiscalCode = taxAuthority.registerInvoice(invoiceId, tariff.totalAmountCents(), tariff.taxCents());

        // 4. Criação e arquivo legal da fatura (política de retenção de 10 anos)
        String status = tariff.totalAmountCents() == 0 ? "FREE" : "PAID";
        InvoiceDTO invoice = new InvoiceDTO(
                invoiceId,
                request.tripId(),
                request.userId(),
                tariff.totalAmountCents(),
                tariff.taxCents(),
                request.paymentMethod(),
                status,
                atFiscalCode,
                issuedAt
        );

        repository.save(invoice);
        return invoice;
    }

    public Optional<InvoiceDTO> getInvoiceByTripId(String tripId) {
        return repository.findByTripId(tripId);
    }

    public Optional<InvoiceDTO> getInvoiceById(String invoiceId) {
        return repository.findById(invoiceId);
    }

    public List<InvoiceDTO> getUserInvoices(String userId) {
        return repository.findByUserId(userId);
    }
}
