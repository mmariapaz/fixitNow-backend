package com.fixitnow.backend.solicitud;

import java.time.LocalDateTime;

public record SolicitudPublicadaResponse(
        long id,
        String estado,
        long direccionId,
        LocalDateTime fechaTentativa,
        NivelUrgencia nivelUrgencia
) {
}
