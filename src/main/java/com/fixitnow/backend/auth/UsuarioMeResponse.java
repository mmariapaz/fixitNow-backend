package com.fixitnow.backend.auth;

import com.fixitnow.backend.usuario.RolUsuario;

public record UsuarioMeResponse(
        long id,
        String correo,
        RolUsuario rol
) {
}
