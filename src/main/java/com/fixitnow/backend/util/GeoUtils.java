package com.fixitnow.backend.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class GeoUtils {

    public static final double RADIO_TIERRA_KM = 6371.0;

    private GeoUtils() {
        // Clase de utilidades estáticas
    }

    /**
     * Calcula la distancia en kilómetros entre dos coordenadas geográficas
     * utilizando la fórmula del semiverseno (Haversine).
     */
    public static double calcularDistanciaKm(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double lat1Rad = Math.toRadians(lat1);
        double lat2Rad = Math.toRadians(lat2);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(lat1Rad) * Math.cos(lat2Rad)
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return RADIO_TIERRA_KM * c;
    }

    /**
     * Redondea un valor decimal a exactamente 2 posiciones decimales
     * aplicando el criterio estándar HALF_UP.
     */
    public static double redondearDosDecimales(double valor) {
        return BigDecimal.valueOf(valor)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
    }
}
