package com.fixitnow.backend.direccion;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "direcciones")
public class Direccion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long clienteId;

    @Column(nullable = false, length = 100)
    private String calle;

    @Column(nullable = false, length = 20)
    private String numero;

    @Column(nullable = true, length = 20)
    private String pisoDepto;

    @Column(nullable = false, length = 100)
    private String ciudad;

    @Column(nullable = true, length = 20)
    private String codigoPostal;

    @Column(nullable = false)
    private Double latitud;

    @Column(nullable = false)
    private Double longitud;

    protected Direccion() {
        // Requerido por JPA
    }

    public Direccion(Long clienteId, String calle, String numero, String pisoDepto,
                     String ciudad, String codigoPostal, Double latitud, Double longitud) {
        this.clienteId = clienteId;
        this.calle = calle;
        this.numero = numero;
        this.pisoDepto = pisoDepto;
        this.ciudad = ciudad;
        this.codigoPostal = codigoPostal;
        this.latitud = latitud;
        this.longitud = longitud;
    }

    public Direccion(Long id, Long clienteId, String calle, String numero, String pisoDepto,
                     String ciudad, String codigoPostal, Double latitud, Double longitud) {
        this.id = id;
        this.clienteId = clienteId;
        this.calle = calle;
        this.numero = numero;
        this.pisoDepto = pisoDepto;
        this.ciudad = ciudad;
        this.codigoPostal = codigoPostal;
        this.latitud = latitud;
        this.longitud = longitud;
    }

    public Long getId() {
        return id;
    }

    public Long getClienteId() {
        return clienteId;
    }

    public String getCalle() {
        return calle;
    }

    public String getNumero() {
        return numero;
    }

    public String getPisoDepto() {
        return pisoDepto;
    }

    public String getCiudad() {
        return ciudad;
    }

    public String getCodigoPostal() {
        return codigoPostal;
    }

    public Double getLatitud() {
        return latitud;
    }

    public Double getLongitud() {
        return longitud;
    }
}
