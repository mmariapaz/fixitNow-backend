package com.fixitnow.backend.profesional;

import java.util.Set;

public record PerfilProfesionalResponse(
        long id,
        long profesionalId,
        Set<Long> categoriaIds,
        String matriculaLicencia,
        String descripcionExperiencia,
        double latitudBase,
        double longitudBase,
        int radioCoberturaKm
) {
}
