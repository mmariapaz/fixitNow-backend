package com.fixitnow.backend.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class GeoUtilsTests {

    @Test
    void distanciaEntreMismoPuntoEsCero() {
        double d = GeoUtils.calcularDistanciaKm(-34.6037, -58.3816, -34.6037, -58.3816);
        assertEquals(0.0, d, 0.0001);
    }

    @Test
    void distanciaBuenosAiresLaPlataEsAproximadamente54Km() {
        // Obelisco Buenos Aires (-34.6037, -58.3816) y Plaza Moreno La Plata (-34.9214, -57.9545)
        double distancia = GeoUtils.calcularDistanciaKm(-34.6037, -58.3816, -34.9214, -57.9545);
        // Distancia geodésica exacta por Haversine es ~52.63 km
        assertTrue(distancia > 52.0 && distancia < 54.0, "La distancia debe estar entre 52 y 54 km");
        double redondeada = GeoUtils.redondearDosDecimales(distancia);
        assertEquals(52.63, redondeada, 0.05);
    }

    @Test
    void redondearDosDecimalesFuncionaCorrectamente() {
        assertEquals(12.35, GeoUtils.redondearDosDecimales(12.345));
        assertEquals(12.34, GeoUtils.redondearDosDecimales(12.344));
        assertEquals(5.00, GeoUtils.redondearDosDecimales(5.0));
    }
}
