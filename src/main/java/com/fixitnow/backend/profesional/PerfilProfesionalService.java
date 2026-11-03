package com.fixitnow.backend.profesional;

import java.util.HashSet;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.fixitnow.backend.categoria.CategoriaService;
import com.fixitnow.backend.usuario.RolUsuario;
import com.fixitnow.backend.usuario.Usuario;
import com.fixitnow.backend.usuario.UsuarioRepository;

@Service
public class PerfilProfesionalService {

    private final PerfilProfesionalRepository perfilRepository;
    private final UsuarioRepository usuarioRepository;
    private final CategoriaService categoriaService;

    public PerfilProfesionalService(PerfilProfesionalRepository perfilRepository,
                                   UsuarioRepository usuarioRepository,
                                   CategoriaService categoriaService) {
        this.perfilRepository = perfilRepository;
        this.usuarioRepository = usuarioRepository;
        this.categoriaService = categoriaService;
    }

    @Transactional
    public PerfilProfesionalResponse crearOActualizarPerfil(PerfilProfesionalRequest request, Long profesionalId) {
        Usuario usuario = usuarioRepository.findById(profesionalId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario autenticado no encontrado."));

        if (usuario.getRol() != RolUsuario.PROFESIONAL) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo las cuentas profesionales pueden configurar su perfil.");
        }

        validarEspecialidades(request.categoriaIds());

        Set<Long> categoriasUnicas = new HashSet<>(request.categoriaIds());

        PerfilProfesional perfil = perfilRepository.findByProfesionalId(profesionalId)
                .map(existente -> {
                    existente.actualizar(
                            categoriasUnicas,
                            request.matriculaLicencia() != null ? request.matriculaLicencia().trim() : null,
                            request.descripcionExperiencia() != null ? request.descripcionExperiencia().trim() : null,
                            request.latitudBase().doubleValue(),
                            request.longitudBase().doubleValue(),
                            request.radioCoberturaKm()
                    );
                    return existente;
                })
                .orElseGet(() -> new PerfilProfesional(
                        profesionalId,
                        categoriasUnicas,
                        request.matriculaLicencia() != null ? request.matriculaLicencia().trim() : null,
                        request.descripcionExperiencia() != null ? request.descripcionExperiencia().trim() : null,
                        request.latitudBase().doubleValue(),
                        request.longitudBase().doubleValue(),
                        request.radioCoberturaKm()
                ));

        PerfilProfesional guardado = perfilRepository.save(perfil);
        return mapearADto(guardado);
    }

    @Transactional(readOnly = true)
    public PerfilProfesionalResponse obtenerPerfil(Long profesionalId) {
        Usuario usuario = usuarioRepository.findById(profesionalId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario autenticado no encontrado."));

        if (usuario.getRol() != RolUsuario.PROFESIONAL) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo las cuentas profesionales pueden consultar su perfil.");
        }

        PerfilProfesional perfil = perfilRepository.findByProfesionalId(profesionalId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "El perfil profesional aún no ha sido configurado."));

        return mapearADto(perfil);
    }

    private void validarEspecialidades(java.util.List<Long> categoriaIds) {
        if (categoriaIds == null || categoriaIds.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debe indicar al menos una especialidad.");
        }

        Set<Long> set = new HashSet<>();
        for (Long id : categoriaIds) {
            if (id == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El identificador de categoría no puede ser nulo.");
            }
            if (!set.add(id)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No se permiten identificadores de categoría repetidos.");
            }
            if (categoriaService.buscarPorId(id).isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La categoría " + id + " no existe en el catálogo.");
            }
        }
    }

    private PerfilProfesionalResponse mapearADto(PerfilProfesional p) {
        return new PerfilProfesionalResponse(
                p.getId(),
                p.getProfesionalId(),
                p.getCategoriaIds(),
                p.getMatriculaLicencia(),
                p.getDescripcionExperiencia(),
                p.getLatitudBase(),
                p.getLongitudBase(),
                p.getRadioCoberturaKm()
        );
    }
}
