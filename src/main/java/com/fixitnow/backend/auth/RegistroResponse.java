package com.fixitnow.backend.auth;

import com.fixitnow.backend.usuario.RolUsuario;

public record RegistroResponse(
        long id,
        String correo,
        RolUsuario rol
) {
}
