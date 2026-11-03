package com.fixitnow.backend.profesional;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PerfilProfesionalRepository extends JpaRepository<PerfilProfesional, Long> {
    Optional<PerfilProfesional> findByProfesionalId(Long profesionalId);
    boolean existsByProfesionalId(Long profesionalId);
}
