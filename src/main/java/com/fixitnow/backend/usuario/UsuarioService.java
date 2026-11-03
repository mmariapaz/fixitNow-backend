package com.fixitnow.backend.usuario;

import java.util.Locale;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.fixitnow.backend.auth.LoginRequest;
import com.fixitnow.backend.auth.LoginResponse;
import com.fixitnow.backend.auth.RegistroRequest;
import com.fixitnow.backend.auth.RegistroResponse;
import com.fixitnow.backend.auth.UsuarioMeResponse;
import com.fixitnow.backend.security.JwtService;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public RegistroResponse registrar(RegistroRequest request) {
        if (request.correo() == null || request.correo().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El correo es obligatorio.");
        }

        String correoNormalizado = normalizarCorreo(request.correo());

        if (usuarioRepository.existsByCorreo(correoNormalizado)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El correo especificado ya está registrado.");
        }

        String contrasenaHash = passwordEncoder.encode(request.contrasena());

        Usuario usuario = new Usuario(correoNormalizado, contrasenaHash, request.rol());
        Usuario guardado = usuarioRepository.save(usuario);

        return new RegistroResponse(guardado.getId(), guardado.getCorreo(), guardado.getRol());
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        if (request.correo() == null || request.correo().isBlank() || request.contrasena() == null || request.contrasena().isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciales inválidas.");
        }

        String correoNormalizado = normalizarCorreo(request.correo());

        Usuario usuario = usuarioRepository.findByCorreo(correoNormalizado)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciales inválidas."));

        if (!passwordEncoder.matches(request.contrasena(), usuario.getContrasenaHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciales inválidas.");
        }

        String token = jwtService.generarToken(usuario);
        return new LoginResponse(token, "Bearer", JwtService.EXPIRATION_SECONDS);
    }

    @Transactional(readOnly = true)
    public UsuarioMeResponse obtenerPerfil(Long usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no encontrado."));

        return new UsuarioMeResponse(usuario.getId(), usuario.getCorreo(), usuario.getRol());
    }

    public static String normalizarCorreo(String correo) {
        return correo == null ? null : correo.trim().toLowerCase(Locale.ROOT);
    }
}
