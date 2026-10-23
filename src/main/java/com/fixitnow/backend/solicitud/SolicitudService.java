package com.fixitnow.backend.solicitud;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.fixitnow.backend.categoria.CategoriaService;

@Service
public class SolicitudService {

    private final CategoriaService categoriaService;
    private final List<Solicitud> solicitudes = new CopyOnWriteArrayList<>();
    private final AtomicLong nextId = new AtomicLong(1);

    public SolicitudService(CategoriaService categoriaService) {
        this.categoriaService = categoriaService;
    }

    public SolicitudResponse registrarSolicitudPreliminar(SolicitudRequest request) {
        boolean categoriaExiste = categoriaService.listar().stream()
                .anyMatch(cat -> cat.id() == request.categoriaId());

        if (!categoriaExiste) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La categoría especificada no existe.");
        }

        long id = nextId.getAndIncrement();
        String estado = "BORRADOR";

        Solicitud solicitud = new Solicitud(
                id,
                request.categoriaId(),
                request.titulo(),
                request.descripcion(),
                request.nivelUrgencia(),
                estado
        );

        solicitudes.add(solicitud);

        return new SolicitudResponse(
                solicitud.id(),
                solicitud.categoriaId(),
                solicitud.titulo(),
                solicitud.descripcion(),
                solicitud.nivelUrgencia(),
                solicitud.estado()
        );
    }

    public List<Solicitud> listar() {
        return List.copyOf(solicitudes);
    }

    public Optional<SolicitudEstadoResponse> consultarEstado(long id) {
        return solicitudes.stream()
                .filter(s -> s.id() == id)
                .map(s -> new SolicitudEstadoResponse(s.id(), s.estado()))
                .findFirst();
    }
}
