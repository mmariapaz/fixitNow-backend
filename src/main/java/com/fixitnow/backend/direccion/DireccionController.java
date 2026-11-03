package com.fixitnow.backend.direccion;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/direcciones")
public class DireccionController {

    private final DireccionService direccionService;

    public DireccionController(DireccionService direccionService) {
        this.direccionService = direccionService;
    }

    @PostMapping
    public ResponseEntity<DireccionResponse> crear(
            @Valid @RequestBody DireccionRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        Long clienteId = Long.parseLong(jwt.getSubject());
        DireccionResponse response = direccionService.crear(request, clienteId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<DireccionResponse>> listar(
            @AuthenticationPrincipal Jwt jwt) {
        Long clienteId = Long.parseLong(jwt.getSubject());
        List<DireccionResponse> response = direccionService.listarPorCliente(clienteId);
        return ResponseEntity.ok(response);
    }
}
