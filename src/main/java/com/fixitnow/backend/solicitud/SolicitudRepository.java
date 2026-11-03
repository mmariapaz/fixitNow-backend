package com.fixitnow.backend.solicitud;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SolicitudRepository extends JpaRepository<Solicitud, Long> {
    Optional<Solicitud> findByIdAndClienteId(Long id, Long clienteId);
    List<Solicitud> findAllByClienteId(Long clienteId);
    List<Solicitud> findAllByEstado(String estado);
    List<Solicitud> findAllByEstadoAndCategoriaIdIn(String estado, Collection<Long> categoriaIds);
}
