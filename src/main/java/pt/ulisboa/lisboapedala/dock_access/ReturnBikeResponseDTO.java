package pt.ulisboa.lisboapedala.dock_access;

/**
 * Contrato público para resposta à devolução de bicicleta.
 */
public record ReturnBikeResponseDTO(
        boolean success,
        String tripId,
        String message
) {}
