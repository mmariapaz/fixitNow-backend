package com.fixitnow.backend.direccion;

public record DireccionResponse(
        long id,
        String calle,
        String numero,
        String pisoDepto,
        String ciudad,
        String codigoPostal,
        double latitud,
        double longitud
) {
}
