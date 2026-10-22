package com.fixitnow.backend.solicitud;

public record SolicitudResponse(
        long id,
        long categoriaId,
        String titulo,
        String descripcion,
        NivelUrgencia nivelUrgencia,
        String estado
) {
}
