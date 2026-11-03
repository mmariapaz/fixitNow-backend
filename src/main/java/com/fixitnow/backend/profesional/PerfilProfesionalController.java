package com.fixitnow.backend.profesional;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/profesionales/perfil")
public class PerfilProfesionalController {

    private final PerfilProfesionalService perfilService;

    public PerfilProfesionalController(PerfilProfesionalService perfilService) {
        this.perfilService = perfilService;
    }

    @PutMapping
    public ResponseEntity<PerfilProfesionalResponse> crearOActualizar(
            @Valid @RequestBody PerfilProfesionalRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        Long profesionalId = Long.parseLong(jwt.getSubject());
        PerfilProfesionalResponse response = perfilService.crearOActualizarPerfil(request, profesionalId);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<PerfilProfesionalResponse> obtener(
            @AuthenticationPrincipal Jwt jwt) {
        Long profesionalId = Long.parseLong(jwt.getSubject());
        PerfilProfesionalResponse response = perfilService.obtenerPerfil(profesionalId);
        return ResponseEntity.ok(response);
    }
}
