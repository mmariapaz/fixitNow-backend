package com.fixitnow.backend.profesional;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

@Entity
@Table(name = "perfiles_profesionales")
public class PerfilProfesional {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long profesionalId;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "profesional_especialidades", joinColumns = @JoinColumn(name = "perfil_id"))
    @Column(name = "categoria_id", nullable = false)
    private Set<Long> categoriaIds = new HashSet<>();

    @Column(nullable = true, length = 50)
    private String matriculaLicencia;

    @Column(nullable = true, length = 1000)
    private String descripcionExperiencia;

    @Column(nullable = false)
    private Double latitudBase;

    @Column(nullable = false)
    private Double longitudBase;

    @Column(nullable = false)
    private Integer radioCoberturaKm;

    protected PerfilProfesional() {
        // Requerido por JPA
    }

    public PerfilProfesional(Long profesionalId, Set<Long> categoriaIds, String matriculaLicencia,
                             String descripcionExperiencia, Double latitudBase, Double longitudBase,
                             Integer radioCoberturaKm) {
        this.profesionalId = profesionalId;
        this.categoriaIds = categoriaIds != null ? new HashSet<>(categoriaIds) : new HashSet<>();
        this.matriculaLicencia = matriculaLicencia;
        this.descripcionExperiencia = descripcionExperiencia;
        this.latitudBase = latitudBase;
        this.longitudBase = longitudBase;
        this.radioCoberturaKm = radioCoberturaKm;
    }

    public void actualizar(Set<Long> nuevasCategorias, String matriculaLicencia,
                           String descripcionExperiencia, Double latitudBase,
                           Double longitudBase, Integer radioCoberturaKm) {
        this.categoriaIds.clear();
        if (nuevasCategorias != null) {
            this.categoriaIds.addAll(nuevasCategorias);
        }
        this.matriculaLicencia = matriculaLicencia;
        this.descripcionExperiencia = descripcionExperiencia;
        this.latitudBase = latitudBase;
        this.longitudBase = longitudBase;
        this.radioCoberturaKm = radioCoberturaKm;
    }

    public Long getId() {
        return id;
    }

    public Long getProfesionalId() {
        return profesionalId;
    }

    public Set<Long> getCategoriaIds() {
        return categoriaIds;
    }

    public String getMatriculaLicencia() {
        return matriculaLicencia;
    }

    public String getDescripcionExperiencia() {
        return descripcionExperiencia;
    }

    public Double getLatitudBase() {
        return latitudBase;
    }

    public Double getLongitudBase() {
        return longitudBase;
    }

    public Integer getRadioCoberturaKm() {
        return radioCoberturaKm;
    }
}
