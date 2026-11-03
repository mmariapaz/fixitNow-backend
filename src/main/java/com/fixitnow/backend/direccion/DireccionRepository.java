package com.fixitnow.backend.direccion;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DireccionRepository extends JpaRepository<Direccion, Long> {
    List<Direccion> findAllByClienteId(Long clienteId);
    Optional<Direccion> findByIdAndClienteId(Long id, Long clienteId);
}
