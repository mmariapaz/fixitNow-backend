package com.fixitnow.backend.solicitud;

public record Solicitud(
        long id,
        long categoriaId,
        String titulo,
        String descripcion,
        NivelUrgencia nivelUrgencia,
        String estado
) {
}
