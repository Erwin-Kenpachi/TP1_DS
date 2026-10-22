package pt.ulisboa.lisboapedala.dock_access;

/**
 * Contrato público para resposta ao pedido de desbloqueio.
 */
public record UnlockBikeResponseDTO(
        boolean success,
        String tripId,
        String message
) {}
