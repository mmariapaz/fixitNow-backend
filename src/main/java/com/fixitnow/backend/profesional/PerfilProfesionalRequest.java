package com.fixitnow.backend.profesional;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PerfilProfesionalRequest(
        @NotEmpty(message = "Debe indicar al menos una especialidad")
        List<Long> categoriaIds,

        @Size(max = 50, message = "La matrícula o licencia no puede superar los 50 caracteres")
        String matriculaLicencia,

        @Size(max = 1000, message = "La descripción de experiencia no puede superar los 1000 caracteres")
        String descripcionExperiencia,

        @NotNull(message = "La latitud base es obligatoria")
        @DecimalMin(value = "-90.0", message = "La latitud base no puede ser menor a -90")
        @DecimalMax(value = "90.0", message = "La latitud base no puede ser mayor a 90")
        BigDecimal latitudBase,

        @NotNull(message = "La longitud base es obligatoria")
        @DecimalMin(value = "-180.0", message = "La longitud base no puede ser menor a -180")
        @DecimalMax(value = "180.0", message = "La longitud base no puede ser mayor a 180")
        BigDecimal longitudBase,

        @NotNull(message = "El radio de cobertura es obligatorio")
        @Min(value = 1, message = "El radio de cobertura mínimo es 1 km")
        @Max(value = 50, message = "El radio de cobertura máximo es 50 km")
        Integer radioCoberturaKm
) {
}
