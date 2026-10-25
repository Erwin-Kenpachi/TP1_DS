package pt.ulisboa.lisboapedala.reporting_audit.business;

/**
 * Calculador de estimativa de emissões de CO₂ evitadas.
 * Adota a fórmula oficial:
 * - Velocidade média estimada em meio urbano: 15 km/h (0.25 km/minuto).
 * - Fator de emissão de veículo ligeiro convencional a combustão: ~130g de CO₂ por passageiro-km (0.130 kg/km).
 */
public class CarbonSavingsCalculator {

    public static final double AVERAGE_SPEED_KM_PER_MINUTE = 0.25; // 15 km/h
    public static final double CO2_SAVED_KG_PER_KM = 0.130;       // 130g CO2/km

    public double estimateDistanceKm(long durationMinutes) {
        if (durationMinutes <= 0) return 0.0;
        return durationMinutes * AVERAGE_SPEED_KM_PER_MINUTE;
    }

    public double calculateCo2SavedKg(double distanceKm) {
        if (distanceKm <= 0) return 0.0;
        return distanceKm * CO2_SAVED_KG_PER_KM;
    }

    public double calculateCo2SavedFromDuration(long durationMinutes) {
        double distance = estimateDistanceKm(durationMinutes);
        return calculateCo2SavedKg(distance);
    }
}
