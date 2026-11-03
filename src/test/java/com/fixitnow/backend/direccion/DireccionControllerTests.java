package com.fixitnow.backend.direccion;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fixitnow.backend.security.JwtService;
import com.fixitnow.backend.usuario.RolUsuario;
import com.fixitnow.backend.usuario.Usuario;
import com.fixitnow.backend.usuario.UsuarioRepository;

@SpringBootTest
@AutoConfigureMockMvc
class DireccionControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DireccionRepository direccionRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    private Usuario cliente1;
    private Usuario cliente2;
    private Usuario profesional;
    private String tokenCliente1;
    private String tokenCliente2;
    private String tokenProfesional;

    @BeforeEach
    void setUp() {
        direccionRepository.deleteAll();
        usuarioRepository.deleteAll();

        cliente1 = usuarioRepository.save(new Usuario(
                "cliente1.direcciones@ejemplo.com",
                passwordEncoder.encode("claveSegura123"),
                RolUsuario.CLIENTE
        ));
        cliente2 = usuarioRepository.save(new Usuario(
                "cliente2.direcciones@ejemplo.com",
                passwordEncoder.encode("claveSegura123"),
                RolUsuario.CLIENTE
        ));
        profesional = usuarioRepository.save(new Usuario(
                "profesional.direcciones@ejemplo.com",
                passwordEncoder.encode("claveSegura123"),
                RolUsuario.PROFESIONAL
        ));

        tokenCliente1 = jwtService.generarToken(cliente1);
        tokenCliente2 = jwtService.generarToken(cliente2);
        tokenProfesional = jwtService.generarToken(profesional);
    }

    @Test
    void clienteCreaDireccionValidaDevuelve201ConIdYDatosSinExponerClienteId() throws Exception {
        String payload = """
                {
                    "calle": "Av. Corrientes",
                    "numero": "1234",
                    "pisoDepto": "4B",
                    "ciudad": "Buenos Aires",
                    "codigoPostal": "C1043",
                    "latitud": -34.6037,
                    "longitud": -58.3816
                }
                """;

        mockMvc.perform(post("/api/v1/direcciones")
                        .header("Authorization", "Bearer " + tokenCliente1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.calle").value("Av. Corrientes"))
                .andExpect(jsonPath("$.numero").value("1234"))
                .andExpect(jsonPath("$.pisoDepto").value("4B"))
                .andExpect(jsonPath("$.ciudad").value("Buenos Aires"))
                .andExpect(jsonPath("$.codigoPostal").value("C1043"))
                .andExpect(jsonPath("$.latitud").value(-34.6037))
                .andExpect(jsonPath("$.longitud").value(-58.3816))
                .andExpect(jsonPath("$.clienteId").doesNotExist());
    }

    @Test
    void listarDireccionesDevuelveSoloLasDelClienteAutenticado() throws Exception {
        // Crear dirección de cliente 1
        String payload1 = """
                {
                    "calle": "Calle 1",
                    "numero": "100",
                    "ciudad": "La Plata",
                    "latitud": -34.9214,
                    "longitud": -57.9545
                }
                """;
        mockMvc.perform(post("/api/v1/direcciones")
                        .header("Authorization", "Bearer " + tokenCliente1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload1))
                .andExpect(status().isCreated());

        // Crear dirección de cliente 2
        String payload2 = """
                {
                    "calle": "Calle 2",
                    "numero": "200",
                    "ciudad": "Rosario",
                    "latitud": -32.9468,
                    "longitud": -60.6393
                }
                """;
        mockMvc.perform(post("/api/v1/direcciones")
                        .header("Authorization", "Bearer " + tokenCliente2)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload2))
                .andExpect(status().isCreated());

        // Cliente 1 debe ver solo 1 dirección (Calle 1)
        mockMvc.perform(get("/api/v1/direcciones")
                        .header("Authorization", "Bearer " + tokenCliente1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].calle").value("Calle 1"));

        // Cliente 2 debe ver solo 1 dirección (Calle 2)
        mockMvc.perform(get("/api/v1/direcciones")
                        .header("Authorization", "Bearer " + tokenCliente2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].calle").value("Calle 2"));
    }

    @Test
    void listarDireccionesCuandoNoTieneDevuelve200ConArrayVacio() throws Exception {
        mockMvc.perform(get("/api/v1/direcciones")
                        .header("Authorization", "Bearer " + tokenCliente1))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    void profesionalRecibe403AlIntentarCrearOListarDirecciones() throws Exception {
        String payload = """
                {
                    "calle": "Calle Profesional",
                    "numero": "500",
                    "ciudad": "Córdoba",
                    "latitud": -31.4201,
                    "longitud": -64.1888
                }
                """;

        mockMvc.perform(post("/api/v1/direcciones")
                        .header("Authorization", "Bearer " + tokenProfesional)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/direcciones")
                        .header("Authorization", "Bearer " + tokenProfesional))
                .andExpect(status().isForbidden());
    }

    @Test
    void peticionesSinTokenDevuelven401() throws Exception {
        mockMvc.perform(get("/api/v1/direcciones"))
                .andExpect(status().isUnauthorized());

        String payload = """
                {
                    "calle": "Calle Sin Token",
                    "numero": "100",
                    "ciudad": "Mendoza",
                    "latitud": -32.8895,
                    "longitud": -68.8458
                }
                """;

        mockMvc.perform(post("/api/v1/direcciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rechazarLatitudYLongitudFueraDeRangoDevuelve400() throws Exception {
        // Latitud > 90
        String latitudInvalida = """
                {
                    "calle": "Calle Test",
                    "numero": "100",
                    "ciudad": "Test",
                    "latitud": 90.1,
                    "longitud": 0.0
                }
                """;
        mockMvc.perform(post("/api/v1/direcciones")
                        .header("Authorization", "Bearer " + tokenCliente1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(latitudInvalida))
                .andExpect(status().isBadRequest());

        // Latitud < -90
        String latitudMenor = """
                {
                    "calle": "Calle Test",
                    "numero": "100",
                    "ciudad": "Test",
                    "latitud": -90.5,
                    "longitud": 0.0
                }
                """;
        mockMvc.perform(post("/api/v1/direcciones")
                        .header("Authorization", "Bearer " + tokenCliente1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(latitudMenor))
                .andExpect(status().isBadRequest());

        // Longitud > 180
        String longitudInvalida = """
                {
                    "calle": "Calle Test",
                    "numero": "100",
                    "ciudad": "Test",
                    "latitud": 0.0,
                    "longitud": 180.1
                }
                """;
        mockMvc.perform(post("/api/v1/direcciones")
                        .header("Authorization", "Bearer " + tokenCliente1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(longitudInvalida))
                .andExpect(status().isBadRequest());

        // Longitud < -180
        String longitudMenor = """
                {
                    "calle": "Calle Test",
                    "numero": "100",
                    "ciudad": "Test",
                    "latitud": 0.0,
                    "longitud": -180.5
                }
                """;
        mockMvc.perform(post("/api/v1/direcciones")
                        .header("Authorization", "Bearer " + tokenCliente1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(longitudMenor))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rechazarCamposObligatoriosFaltantesOEnBlancoDevuelve400() throws Exception {
        String sinCalle = """
                {
                    "numero": "100",
                    "ciudad": "Test",
                    "latitud": 0.0,
                    "longitud": 0.0
                }
                """;
        mockMvc.perform(post("/api/v1/direcciones")
                        .header("Authorization", "Bearer " + tokenCliente1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(sinCalle))
                .andExpect(status().isBadRequest());

        String sinNumero = """
                {
                    "calle": "Calle Test",
                    "ciudad": "Test",
                    "latitud": 0.0,
                    "longitud": 0.0
                }
                """;
        mockMvc.perform(post("/api/v1/direcciones")
                        .header("Authorization", "Bearer " + tokenCliente1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(sinNumero))
                .andExpect(status().isBadRequest());

        String sinCiudad = """
                {
                    "calle": "Calle Test",
                    "numero": "100",
                    "latitud": 0.0,
                    "longitud": 0.0
                }
                """;
        mockMvc.perform(post("/api/v1/direcciones")
                        .header("Authorization", "Bearer " + tokenCliente1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(sinCiudad))
                .andExpect(status().isBadRequest());
    }

    @Test
    void crearDireccionIgnoraClienteIdEnviadoEnElCuerpoYAsociaAlToken() throws Exception {
        String payload = """
                {
                    "calle": "Calle Segura",
                    "numero": "777",
                    "ciudad": "Salta",
                    "latitud": -24.7821,
                    "longitud": -65.4232,
                    "clienteId": %d
                }
                """.formatted(cliente2.getId());

        MvcResult result = mockMvc.perform(post("/api/v1/direcciones")
                        .header("Authorization", "Bearer " + tokenCliente1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        long id = json.get("id").asLong();

        Direccion guardada = direccionRepository.findById(id).orElseThrow();
        assertEquals(cliente1.getId(), guardada.getClienteId(), "La dirección debe pertenecer al cliente autenticado");
    }
}
