package com.fixitnow.backend.auth;

import java.util.Locale;

import com.fixitnow.backend.usuario.RolUsuario;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegistroRequest(
        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "El formato de correo no es válido")
        String correo,

        @NotBlank(message = "La contraseña es obligatoria")
        @Size(min = 8, max = 70, message = "La contraseña debe tener entre 8 y 70 caracteres")
        String contrasena,

        @NotNull(message = "El rol es obligatorio")
        RolUsuario rol
) {
    public RegistroRequest {
        if (correo != null) {
            correo = correo.trim();
        }
    }
}
