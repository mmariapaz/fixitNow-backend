package com.fixitnow.backend.categoria;

import java.util.List;

import org.springframework.stereotype.Service;

@Service
public class CategoriaService {

    private final List<Categoria> categorias = List.of(
            new Categoria(1, "Plomería"),
            new Categoria(2, "Electricidad"),
            new Categoria(3, "Gas"),
            new Categoria(4, "Cerrajería"),
            new Categoria(5, "Albañilería"),
            new Categoria(6, "Pintura"));

    public List<Categoria> listar() {
        return categorias;
    }
}
