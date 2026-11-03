package com.fixitnow.backend.solicitud;

import java.time.LocalDateTime;

public record SolicitudCercanaResponse(
        long id,
        long categoriaId,
        String categoriaNombre,
        String titulo,
        String descripcion,
        NivelUrgencia nivelUrgencia,
        LocalDateTime fechaTentativa,
        double distanciaKm
) {
}
