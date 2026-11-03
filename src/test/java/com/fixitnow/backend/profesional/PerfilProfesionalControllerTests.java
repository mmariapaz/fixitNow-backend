package com.fixitnow.backend.profesional;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import com.fixitnow.backend.security.JwtService;
import com.fixitnow.backend.usuario.RolUsuario;
import com.fixitnow.backend.usuario.Usuario;
import com.fixitnow.backend.usuario.UsuarioRepository;

@SpringBootTest
@AutoConfigureMockMvc
class PerfilProfesionalControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PerfilProfesionalRepository perfilRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    private Usuario profesional1;
    private Usuario profesional2;
    private Usuario cliente;
    private String tokenProfesional1;
    private String tokenProfesional2;
    private String tokenCliente;

    @BeforeEach
    void setUp() {
        perfilRepository.deleteAll();
        usuarioRepository.deleteAll();

        profesional1 = usuarioRepository.save(new Usuario(
                "profesional1.perfil@ejemplo.com",
                passwordEncoder.encode("claveSegura123"),
                RolUsuario.PROFESIONAL
        ));
        profesional2 = usuarioRepository.save(new Usuario(
                "profesional2.perfil@ejemplo.com",
                passwordEncoder.encode("claveSegura123"),
                RolUsuario.PROFESIONAL
        ));
        cliente = usuarioRepository.save(new Usuario(
                "cliente.perfil@ejemplo.com",
                passwordEncoder.encode("claveSegura123"),
                RolUsuario.CLIENTE
        ));

        tokenProfesional1 = jwtService.generarToken(profesional1);
        tokenProfesional2 = jwtService.generarToken(profesional2);
        tokenCliente = jwtService.generarToken(cliente);
    }

    @Test
    void profesionalCreaPerfilExitosamenteDevuelve200ConDto() throws Exception {
        String payload = """
                {
                    "categoriaIds": [1, 2],
                    "matriculaLicencia": "MAT-2024-ABC",
                    "descripcionExperiencia": "10 años en plomería y electricidad residencial.",
                    "latitudBase": -34.6037,
                    "longitudBase": -58.3816,
                    "radioCoberturaKm": 15
                }
                """;

        mockMvc.perform(put("/api/v1/profesionales/perfil")
                        .header("Authorization", "Bearer " + tokenProfesional1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.profesionalId").value(profesional1.getId()))
                .andExpect(jsonPath("$.categoriaIds", containsInAnyOrder(1, 2)))
                .andExpect(jsonPath("$.matriculaLicencia").value("MAT-2024-ABC"))
                .andExpect(jsonPath("$.descripcionExperiencia").value("10 años en plomería y electricidad residencial."))
                .andExpect(jsonPath("$.latitudBase").value(-34.6037))
                .andExpect(jsonPath("$.longitudBase").value(-58.3816))
                .andExpect(jsonPath("$.radioCoberturaKm").value(15));
    }

    @Test
    void profesionalActualizaPerfilExistenteDevuelve200ConDatosNuevos() throws Exception {
        // Creación inicial
        String inicial = """
                {
                    "categoriaIds": [1],
                    "matriculaLicencia": "MAT-INI",
                    "descripcionExperiencia": "Inicial",
                    "latitudBase": -34.6037,
                    "longitudBase": -58.3816,
                    "radioCoberturaKm": 10
                }
                """;
        mockMvc.perform(put("/api/v1/profesionales/perfil")
                        .header("Authorization", "Bearer " + tokenProfesional1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(inicial))
                .andExpect(status().isOk());

        // Actualización
        String actualizado = """
                {
                    "categoriaIds": [2, 3],
                    "matriculaLicencia": "MAT-ACTUALIZADA",
                    "descripcionExperiencia": "Experiencia actualizada con más servicios.",
                    "latitudBase": -34.5800,
                    "longitudBase": -58.4200,
                    "radioCoberturaKm": 25
                }
                """;
        mockMvc.perform(put("/api/v1/profesionales/perfil")
                        .header("Authorization", "Bearer " + tokenProfesional1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(actualizado))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categoriaIds", containsInAnyOrder(2, 3)))
                .andExpect(jsonPath("$.matriculaLicencia").value("MAT-ACTUALIZADA"))
                .andExpect(jsonPath("$.descripcionExperiencia").value("Experiencia actualizada con más servicios."))
                .andExpect(jsonPath("$.latitudBase").value(-34.5800))
                .andExpect(jsonPath("$.longitudBase").value(-58.4200))
                .andExpect(jsonPath("$.radioCoberturaKm").value(25));

        // GET posterior verifica que los cambios persistan
        mockMvc.perform(get("/api/v1/profesionales/perfil")
                        .header("Authorization", "Bearer " + tokenProfesional1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matriculaLicencia").value("MAT-ACTUALIZADA"))
                .andExpect(jsonPath("$.radioCoberturaKm").value(25));

        // Verificamos que no se crearon registros duplicados
        assertEquals(1, perfilRepository.count(), "Debe existir un único perfil por profesional");
    }

    @Test
    void obtenerPerfilSinHaberloConfiguradoDevuelve404() throws Exception {
        mockMvc.perform(get("/api/v1/profesionales/perfil")
                        .header("Authorization", "Bearer " + tokenProfesional1))
                .andExpect(status().isNotFound());
    }

    @Test
    void aislamientoTotalEntreProfesionales() throws Exception {
        // Prof 1 configura perfil
        String payload1 = """
                {
                    "categoriaIds": [1],
                    "latitudBase": -34.6037,
                    "longitudBase": -58.3816,
                    "radioCoberturaKm": 10
                }
                """;
        mockMvc.perform(put("/api/v1/profesionales/perfil")
                        .header("Authorization", "Bearer " + tokenProfesional1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload1))
                .andExpect(status().isOk());

        // Prof 2 configura perfil
        String payload2 = """
                {
                    "categoriaIds": [2, 3],
                    "latitudBase": -31.4201,
                    "longitudBase": -64.1888,
                    "radioCoberturaKm": 30
                }
                """;
        mockMvc.perform(put("/api/v1/profesionales/perfil")
                        .header("Authorization", "Bearer " + tokenProfesional2)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload2))
                .andExpect(status().isOk());

        // Consultar como Prof 1 devuelve los datos de Prof 1
        mockMvc.perform(get("/api/v1/profesionales/perfil")
                        .header("Authorization", "Bearer " + tokenProfesional1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profesionalId").value(profesional1.getId()))
                .andExpect(jsonPath("$.radioCoberturaKm").value(10))
                .andExpect(jsonPath("$.categoriaIds", hasSize(1)));

        // Consultar como Prof 2 devuelve los datos de Prof 2
        mockMvc.perform(get("/api/v1/profesionales/perfil")
                        .header("Authorization", "Bearer " + tokenProfesional2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profesionalId").value(profesional2.getId()))
                .andExpect(jsonPath("$.radioCoberturaKm").value(30))
                .andExpect(jsonPath("$.categoriaIds", hasSize(2)));
    }

    @Test
    void clienteIntentandoAccederARutasProfesionalesDevuelve403() throws Exception {
        String payload = """
                {
                    "categoriaIds": [1],
                    "latitudBase": -34.6037,
                    "longitudBase": -58.3816,
                    "radioCoberturaKm": 10
                }
                """;
        mockMvc.perform(put("/api/v1/profesionales/perfil")
                        .header("Authorization", "Bearer " + tokenCliente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/profesionales/perfil")
                        .header("Authorization", "Bearer " + tokenCliente))
                .andExpect(status().isForbidden());
    }

    @Test
    void peticionesSinAutenticacionDevuelven401() throws Exception {
        mockMvc.perform(get("/api/v1/profesionales/perfil"))
                .andExpect(status().isUnauthorized());

        String payload = """
                {
                    "categoriaIds": [1],
                    "latitudBase": -34.6037,
                    "longitudBase": -58.3816,
                    "radioCoberturaKm": 10
                }
                """;
        mockMvc.perform(put("/api/v1/profesionales/perfil")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rechazarCategoriasInexistentesODuplicadasDevuelve400() throws Exception {
        // Categoría inexistente en catálogo (ej. 99999)
        String categoriaInexistente = """
                {
                    "categoriaIds": [99999],
                    "latitudBase": -34.6037,
                    "longitudBase": -58.3816,
                    "radioCoberturaKm": 10
                }
                """;
        mockMvc.perform(put("/api/v1/profesionales/perfil")
                        .header("Authorization", "Bearer " + tokenProfesional1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(categoriaInexistente))
                .andExpect(status().isBadRequest());

        // Categorías duplicadas en el arreglo
        String categoriasDuplicadas = """
                {
                    "categoriaIds": [1, 1],
                    "latitudBase": -34.6037,
                    "longitudBase": -58.3816,
                    "radioCoberturaKm": 10
                }
                """;
        mockMvc.perform(put("/api/v1/profesionales/perfil")
                        .header("Authorization", "Bearer " + tokenProfesional1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(categoriasDuplicadas))
                .andExpect(status().isBadRequest());

        // Arreglo vacío de categorías
        String categoriasVacias = """
                {
                    "categoriaIds": [],
                    "latitudBase": -34.6037,
                    "longitudBase": -58.3816,
                    "radioCoberturaKm": 10
                }
                """;
        mockMvc.perform(put("/api/v1/profesionales/perfil")
                        .header("Authorization", "Bearer " + tokenProfesional1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(categoriasVacias))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rechazarCoordenadasORadioFueraDeRangoDevuelve400() throws Exception {
        // Latitud > 90
        String latitudMayor = """
                {
                    "categoriaIds": [1],
                    "latitudBase": 90.1,
                    "longitudBase": 0.0,
                    "radioCoberturaKm": 10
                }
                """;
        mockMvc.perform(put("/api/v1/profesionales/perfil")
                        .header("Authorization", "Bearer " + tokenProfesional1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(latitudMayor))
                .andExpect(status().isBadRequest());

        // Latitud < -90
        String latitudMenor = """
                {
                    "categoriaIds": [1],
                    "latitudBase": -90.1,
                    "longitudBase": 0.0,
                    "radioCoberturaKm": 10
                }
                """;
        mockMvc.perform(put("/api/v1/profesionales/perfil")
                        .header("Authorization", "Bearer " + tokenProfesional1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(latitudMenor))
                .andExpect(status().isBadRequest());

        // Longitud > 180
        String longitudMayor = """
                {
                    "categoriaIds": [1],
                    "latitudBase": 0.0,
                    "longitudBase": 180.1,
                    "radioCoberturaKm": 10
                }
                """;
        mockMvc.perform(put("/api/v1/profesionales/perfil")
                        .header("Authorization", "Bearer " + tokenProfesional1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(longitudMayor))
                .andExpect(status().isBadRequest());

        // Longitud < -180
        String longitudMenor = """
                {
                    "categoriaIds": [1],
                    "latitudBase": 0.0,
                    "longitudBase": -180.1,
                    "radioCoberturaKm": 10
                }
                """;
        mockMvc.perform(put("/api/v1/profesionales/perfil")
                        .header("Authorization", "Bearer " + tokenProfesional1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(longitudMenor))
                .andExpect(status().isBadRequest());

        // Radio < 1
        String radioMenor = """
                {
                    "categoriaIds": [1],
                    "latitudBase": 0.0,
                    "longitudBase": 0.0,
                    "radioCoberturaKm": 0
                }
                """;
        mockMvc.perform(put("/api/v1/profesionales/perfil")
                        .header("Authorization", "Bearer " + tokenProfesional1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(radioMenor))
                .andExpect(status().isBadRequest());

        // Radio > 50
        String radioMayor = """
                {
                    "categoriaIds": [1],
                    "latitudBase": 0.0,
                    "longitudBase": 0.0,
                    "radioCoberturaKm": 51
                }
                """;
        mockMvc.perform(put("/api/v1/profesionales/perfil")
                        .header("Authorization", "Bearer " + tokenProfesional1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(radioMayor))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rechazarCamposObligatoriosFaltantesDevuelve400() throws Exception {
        String sinLatitud = """
                {
                    "categoriaIds": [1],
                    "longitudBase": 0.0,
                    "radioCoberturaKm": 10
                }
                """;
        mockMvc.perform(put("/api/v1/profesionales/perfil")
                        .header("Authorization", "Bearer " + tokenProfesional1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(sinLatitud))
                .andExpect(status().isBadRequest());

        String sinLongitud = """
                {
                    "categoriaIds": [1],
                    "latitudBase": 0.0,
                    "radioCoberturaKm": 10
                }
                """;
        mockMvc.perform(put("/api/v1/profesionales/perfil")
                        .header("Authorization", "Bearer " + tokenProfesional1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(sinLongitud))
                .andExpect(status().isBadRequest());

        String sinRadio = """
                {
                    "categoriaIds": [1],
                    "latitudBase": 0.0,
                    "longitudBase": 0.0
                }
                """;
        mockMvc.perform(put("/api/v1/profesionales/perfil")
                        .header("Authorization", "Bearer " + tokenProfesional1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(sinRadio))
                .andExpect(status().isBadRequest());
    }
}
