package com.fixitnow.backend.solicitud;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotNull;

public record PublicarSolicitudRequest(
        @NotNull(message = "El identificador de dirección es obligatorio")
        Long direccionId,

        @NotNull(message = "La fecha tentativa es obligatoria")
        LocalDateTime fechaTentativa
) {
}
