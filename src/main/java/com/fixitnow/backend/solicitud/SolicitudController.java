package com.fixitnow.backend.solicitud;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/solicitudes")
public class SolicitudController {

    private final SolicitudService solicitudService;

    public SolicitudController(SolicitudService solicitudService) {
        this.solicitudService = solicitudService;
    }

    @PostMapping
    public ResponseEntity<SolicitudResponse> crear(
            @Valid @RequestBody SolicitudRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        Long clienteId = Long.parseLong(jwt.getSubject());
        SolicitudResponse response = solicitudService.registrarSolicitudPreliminar(request, clienteId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<SolicitudResponse>> listarMisSolicitudes(
            @AuthenticationPrincipal Jwt jwt) {
        Long clienteId = Long.parseLong(jwt.getSubject());
        List<SolicitudResponse> response = solicitudService.listarPorCliente(clienteId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/estado")
    public ResponseEntity<SolicitudEstadoResponse> consultarEstado(
            @PathVariable long id,
            @AuthenticationPrincipal Jwt jwt) {
        Long clienteId = Long.parseLong(jwt.getSubject());
        return ResponseEntity.of(solicitudService.consultarEstado(id, clienteId));
    }

    @PatchMapping("/{id}/publicacion")
    public ResponseEntity<SolicitudPublicadaResponse> publicar(
            @PathVariable long id,
            @Valid @RequestBody PublicarSolicitudRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        Long clienteId = Long.parseLong(jwt.getSubject());
        SolicitudPublicadaResponse response = solicitudService.publicarSolicitud(id, request, clienteId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/cercanas")
    public ResponseEntity<List<SolicitudCercanaResponse>> listarCercanas(
            @AuthenticationPrincipal Jwt jwt) {
        Long profesionalId = Long.parseLong(jwt.getSubject());
        List<SolicitudCercanaResponse> response = solicitudService.listarSolicitudesCercanas(profesionalId);
        return ResponseEntity.ok(response);
    }
}
