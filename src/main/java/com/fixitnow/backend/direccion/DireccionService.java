package com.fixitnow.backend.direccion;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.fixitnow.backend.usuario.RolUsuario;
import com.fixitnow.backend.usuario.Usuario;
import com.fixitnow.backend.usuario.UsuarioRepository;

@Service
public class DireccionService {

    private final DireccionRepository direccionRepository;
    private final UsuarioRepository usuarioRepository;

    public DireccionService(DireccionRepository direccionRepository, UsuarioRepository usuarioRepository) {
        this.direccionRepository = direccionRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional
    public DireccionResponse crear(DireccionRequest request, Long clienteId) {
        Usuario cliente = usuarioRepository.findById(clienteId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no encontrado."));

        if (cliente.getRol() != RolUsuario.CLIENTE) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo los clientes pueden registrar direcciones.");
        }

        Direccion direccion = new Direccion(
                clienteId,
                request.calle().trim(),
                request.numero().trim(),
                request.pisoDepto() != null ? request.pisoDepto().trim() : null,
                request.ciudad().trim(),
                request.codigoPostal() != null ? request.codigoPostal().trim() : null,
                request.latitud().doubleValue(),
                request.longitud().doubleValue()
        );

        Direccion guardada = direccionRepository.save(direccion);
        return mapearADto(guardada);
    }

    @Transactional(readOnly = true)
    public List<DireccionResponse> listarPorCliente(Long clienteId) {
        return direccionRepository.findAllByClienteId(clienteId).stream()
                .map(this::mapearADto)
                .toList();
    }

    private DireccionResponse mapearADto(Direccion d) {
        return new DireccionResponse(
                d.getId(),
                d.getCalle(),
                d.getNumero(),
                d.getPisoDepto(),
                d.getCiudad(),
                d.getCodigoPostal(),
                d.getLatitud(),
                d.getLongitud()
        );
    }
}
