package com.fixitnow.backend.direccion;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record DireccionRequest(
        @NotBlank(message = "La calle es obligatoria")
        @Size(min = 1, max = 100, message = "La calle debe tener hasta 100 caracteres")
        String calle,

        @NotBlank(message = "El número es obligatorio")
        @Size(min = 1, max = 20, message = "El número debe tener hasta 20 caracteres")
        String numero,

        @Size(max = 20, message = "El piso o departamento debe tener hasta 20 caracteres")
        String pisoDepto,

        @NotBlank(message = "La ciudad es obligatoria")
        @Size(min = 1, max = 100, message = "La ciudad debe tener hasta 100 caracteres")
        String ciudad,

        @Size(max = 20, message = "El código postal debe tener hasta 20 caracteres")
        String codigoPostal,

        @NotNull(message = "La latitud es obligatoria")
        @DecimalMin(value = "-90.0", message = "La latitud no puede ser menor a -90")
        @DecimalMax(value = "90.0", message = "La latitud no puede ser mayor a 90")
        BigDecimal latitud,

        @NotNull(message = "La longitud es obligatoria")
        @DecimalMin(value = "-180.0", message = "La longitud no puede ser menor a -180")
        @DecimalMax(value = "180.0", message = "La longitud no puede ser mayor a 180")
        BigDecimal longitud
) {
}
