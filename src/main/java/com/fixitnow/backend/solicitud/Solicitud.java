package com.fixitnow.backend.solicitud;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "solicitudes")
public class Solicitud {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long categoriaId;

    @Column(nullable = false, length = 150)
    private String titulo;

    @Column(nullable = false, length = 1000)
    private String descripcion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NivelUrgencia nivelUrgencia;

    @Column(nullable = false)
    private String estado;

    protected Solicitud() {
        // Constructor requerido por JPA
    }

    public Solicitud(Long categoriaId, String titulo, String descripcion, NivelUrgencia nivelUrgencia, String estado) {
        this.categoriaId = categoriaId;
        this.titulo = titulo;
        this.descripcion = descripcion;
        this.nivelUrgencia = nivelUrgencia;
        this.estado = estado;
    }

    public Solicitud(Long id, Long categoriaId, String titulo, String descripcion, NivelUrgencia nivelUrgencia, String estado) {
        this.id = id;
        this.categoriaId = categoriaId;
        this.titulo = titulo;
        this.descripcion = descripcion;
        this.nivelUrgencia = nivelUrgencia;
        this.estado = estado;
    }

    public Long getId() {
        return id;
    }

    public Long getCategoriaId() {
        return categoriaId;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public NivelUrgencia getNivelUrgencia() {
        return nivelUrgencia;
    }

    public String getEstado() {
        return estado;
    }

    public Long id() {
        return id;
    }

    public Long categoriaId() {
        return categoriaId;
    }

    public String titulo() {
        return titulo;
    }

    public String descripcion() {
        return descripcion;
    }

    public NivelUrgencia nivelUrgencia() {
        return nivelUrgencia;
    }

    public String estado() {
        return estado;
    }
}
