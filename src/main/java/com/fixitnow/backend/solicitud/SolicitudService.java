package com.fixitnow.backend.solicitud;

import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.fixitnow.backend.categoria.CategoriaService;

@Service
public class SolicitudService {

    private final CategoriaService categoriaService;
    private final SolicitudRepository solicitudRepository;

    public SolicitudService(CategoriaService categoriaService, SolicitudRepository solicitudRepository) {
        this.categoriaService = categoriaService;
        this.solicitudRepository = solicitudRepository;
    }

    public SolicitudResponse registrarSolicitudPreliminar(SolicitudRequest request) {
        boolean categoriaExiste = categoriaService.listar().stream()
                .anyMatch(cat -> cat.id() == request.categoriaId());

        if (!categoriaExiste) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La categoría especificada no existe.");
        }

        String estado = "BORRADOR";

        Solicitud solicitud = new Solicitud(
                request.categoriaId(),
                request.titulo(),
                request.descripcion(),
                request.nivelUrgencia(),
                estado
        );

        Solicitud guardada = solicitudRepository.save(solicitud);

        return new SolicitudResponse(
                guardada.getId(),
                guardada.getCategoriaId(),
                guardada.getTitulo(),
                guardada.getDescripcion(),
                guardada.getNivelUrgencia(),
                guardada.getEstado()
        );
    }

    public List<Solicitud> listar() {
        return solicitudRepository.findAll();
    }

    public Optional<SolicitudEstadoResponse> consultarEstado(long id) {
        return solicitudRepository.findById(id)
                .map(s -> new SolicitudEstadoResponse(s.getId(), s.getEstado()));
    }
}
