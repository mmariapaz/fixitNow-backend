package com.fixitnow.backend.solicitud;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SolicitudRequest(
        @NotNull(message = "La categoría es obligatoria")
        Long categoriaId,

        @NotBlank(message = "El título es obligatorio")
        @Size(max = 150, message = "El título no puede superar los 150 caracteres")
        String titulo,

        @NotBlank(message = "La descripción es obligatoria")
        String descripcion,

        @NotNull(message = "El nivel de urgencia es obligatorio")
        NivelUrgencia nivelUrgencia
) {
}
