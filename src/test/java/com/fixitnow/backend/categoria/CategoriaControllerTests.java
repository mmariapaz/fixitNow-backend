package com.fixitnow.backend.categoria;

import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class CategoriaControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void devuelveLasSeisCategoriasConIdentificadorYNombre() throws Exception {
        mockMvc.perform(get("/api/v1/categorias"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[*].id", contains(1, 2, 3, 4, 5, 6)))
                .andExpect(jsonPath("$[*].nombre", contains(
                        "Plomería", "Electricidad", "Gas", "Cerrajería", "Albañilería", "Pintura")));
    }

    @Test
    void devuelveCategoriaCuandoExiste() throws Exception {
        mockMvc.perform(get("/api/v1/categorias/1"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.nombre").value("Plomería"));
    }

    @Test
    void devuelve404CuandoCategoriaNoExiste() throws Exception {
        mockMvc.perform(get("/api/v1/categorias/999"))
                .andExpect(status().isNotFound());
    }
}
