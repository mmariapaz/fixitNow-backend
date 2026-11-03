package com.fixitnow.backend.solicitud;

import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.fixitnow.backend.categoria.Categoria;
import com.fixitnow.backend.categoria.CategoriaService;
import com.fixitnow.backend.direccion.Direccion;
import com.fixitnow.backend.direccion.DireccionRepository;
import com.fixitnow.backend.profesional.PerfilProfesional;
import com.fixitnow.backend.profesional.PerfilProfesionalRepository;
import com.fixitnow.backend.usuario.RolUsuario;
import com.fixitnow.backend.usuario.Usuario;
import com.fixitnow.backend.usuario.UsuarioRepository;
import com.fixitnow.backend.util.GeoUtils;

@Service
public class SolicitudService {

    private final CategoriaService categoriaService;
    private final SolicitudRepository solicitudRepository;
    private final UsuarioRepository usuarioRepository;
    private final DireccionRepository direccionRepository;
    private final PerfilProfesionalRepository perfilProfesionalRepository;

    public SolicitudService(CategoriaService categoriaService,
                            SolicitudRepository solicitudRepository,
                            UsuarioRepository usuarioRepository,
                            DireccionRepository direccionRepository,
                            PerfilProfesionalRepository perfilProfesionalRepository) {
        this.categoriaService = categoriaService;
        this.solicitudRepository = solicitudRepository;
        this.usuarioRepository = usuarioRepository;
        this.direccionRepository = direccionRepository;
        this.perfilProfesionalRepository = perfilProfesionalRepository;
    }

    @Transactional
    public SolicitudResponse registrarSolicitudPreliminar(SolicitudRequest request, Long clienteId) {
        Usuario cliente = usuarioRepository.findById(clienteId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario autenticado no encontrado."));

        if (cliente.getRol() != RolUsuario.CLIENTE) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo los clientes pueden registrar solicitudes.");
        }

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
                estado,
                clienteId
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

    @Transactional(readOnly = true)
    public List<Solicitud> listar() {
        return solicitudRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<SolicitudEstadoResponse> consultarEstado(long id, Long clienteId) {
        return solicitudRepository.findByIdAndClienteId(id, clienteId)
                .map(s -> new SolicitudEstadoResponse(s.getId(), s.getEstado()));
    }

    @Transactional
    public SolicitudPublicadaResponse publicarSolicitud(long id, PublicarSolicitudRequest request, Long clienteId) {
        Solicitud solicitud = solicitudRepository.findByIdAndClienteId(id, clienteId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "La solicitud no existe o pertenece a otro cliente."));

        direccionRepository.findByIdAndClienteId(request.direccionId(), clienteId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "La dirección especificada no existe o pertenece a otro cliente."));

        if (!"BORRADOR".equals(solicitud.getEstado())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Solo se pueden publicar solicitudes en estado BORRADOR.");
        }

        solicitud.publicar(request.direccionId(), request.fechaTentativa());
        Solicitud guardada = solicitudRepository.save(solicitud);

        return new SolicitudPublicadaResponse(
                guardada.getId(),
                guardada.getEstado(),
                guardada.getDireccionId(),
                guardada.getFechaTentativa(),
                guardada.getNivelUrgencia()
        );
    }

    @Transactional(readOnly = true)
    public List<SolicitudCercanaResponse> listarSolicitudesCercanas(Long profesionalId) {
        Usuario profesional = usuarioRepository.findById(profesionalId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario autenticado no encontrado."));

        if (profesional.getRol() != RolUsuario.PROFESIONAL) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo las cuentas profesionales pueden consultar solicitudes cercanas.");
        }

        PerfilProfesional perfil = perfilProfesionalRepository.findByProfesionalId(profesionalId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "El profesional no tiene un perfil configurado."));

        if (perfil.getCategoriaIds() == null || perfil.getCategoriaIds().isEmpty()) {
            return List.of();
        }

        List<Solicitud> publicadas = solicitudRepository.findAllByEstadoAndCategoriaIdIn("PUBLICADA", perfil.getCategoriaIds());

        List<SolicitudCercanaResponse> resultado = new java.util.ArrayList<>();
        for (Solicitud s : publicadas) {
            if (s.getDireccionId() == null) {
                continue;
            }
            Optional<Direccion> direccionOpt = direccionRepository.findById(s.getDireccionId());
            if (direccionOpt.isEmpty()) {
                continue;
            }
            Direccion dir = direccionOpt.get();
            double distancia = GeoUtils.calcularDistanciaKm(
                    perfil.getLatitudBase(), perfil.getLongitudBase(),
                    dir.getLatitud(), dir.getLongitud()
            );

            if (distancia <= perfil.getRadioCoberturaKm()) {
                String categoriaNombre = categoriaService.buscarPorId(s.getCategoriaId())
                        .map(Categoria::nombre)
                        .orElse("Desconocida");

                resultado.add(new SolicitudCercanaResponse(
                        s.getId(),
                        s.getCategoriaId(),
                        categoriaNombre,
                        s.getTitulo(),
                        s.getDescripcion(),
                        s.getNivelUrgencia(),
                        s.getFechaTentativa(),
                        GeoUtils.redondearDosDecimales(distancia)
                ));
            }
        }

        return resultado;
    }
}
