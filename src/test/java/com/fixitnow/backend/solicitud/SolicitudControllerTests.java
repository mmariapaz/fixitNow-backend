package com.fixitnow.backend.solicitud;

import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Optional;

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
class SolicitudControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SolicitudRepository solicitudRepository;

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
        solicitudRepository.deleteAll();
        usuarioRepository.deleteAll();

        cliente1 = usuarioRepository.save(new Usuario(
                "cliente1.solicitudes@ejemplo.com",
                passwordEncoder.encode("claveSegura123"),
                RolUsuario.CLIENTE
        ));
        cliente2 = usuarioRepository.save(new Usuario(
                "cliente2.solicitudes@ejemplo.com",
                passwordEncoder.encode("claveSegura123"),
                RolUsuario.CLIENTE
        ));
        profesional = usuarioRepository.save(new Usuario(
                "profesional.solicitudes@ejemplo.com",
                passwordEncoder.encode("claveSegura123"),
                RolUsuario.PROFESIONAL
        ));

        tokenCliente1 = jwtService.generarToken(cliente1);
        tokenCliente2 = jwtService.generarToken(cliente2);
        tokenProfesional = jwtService.generarToken(profesional);
    }

    @Test
    void crearSolicitudValidaDevuelve201ConEstadoBorradorEId() throws Exception {
        String payload = """
                {
                    "categoriaId": 1,
                    "titulo": "Fuga de agua en lavamanos",
                    "descripcion": "El caño de alimentación tiene una pérdida constante.",
                    "nivelUrgencia": "ALTA"
                }
                """;

        mockMvc.perform(post("/api/v1/solicitudes")
                        .header("Authorization", "Bearer " + tokenCliente1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.categoriaId").value(1))
                .andExpect(jsonPath("$.titulo").value("Fuga de agua en lavamanos"))
                .andExpect(jsonPath("$.descripcion").value("El caño de alimentación tiene una pérdida constante."))
                .andExpect(jsonPath("$.nivelUrgencia").value("ALTA"))
                .andExpect(jsonPath("$.estado").value("BORRADOR"));
    }

    @Test
    void dosSolicitudesValidasGeneranIdentificadoresDistintos() throws Exception {
        String payload1 = """
                {
                    "categoriaId": 1,
                    "titulo": "Primera solicitud",
                    "descripcion": "Descripción uno",
                    "nivelUrgencia": "BAJA"
                }
                """;

        String payload2 = """
                {
                    "categoriaId": 2,
                    "titulo": "Segunda solicitud",
                    "descripcion": "Descripción dos",
                    "nivelUrgencia": "MEDIA"
                }
                """;

        MvcResult result1 = mockMvc.perform(post("/api/v1/solicitudes")
                        .header("Authorization", "Bearer " + tokenCliente1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload1))
                .andExpect(status().isCreated())
                .andReturn();

        MvcResult result2 = mockMvc.perform(post("/api/v1/solicitudes")
                        .header("Authorization", "Bearer " + tokenCliente1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload2))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode json1 = objectMapper.readTree(result1.getResponse().getContentAsString());
        JsonNode json2 = objectMapper.readTree(result2.getResponse().getContentAsString());

        long id1 = json1.get("id").asLong();
        long id2 = json2.get("id").asLong();

        assertNotEquals(id1, id2, "Los identificadores generados deben ser distintos");
    }

    @Test
    void crearSolicitudPersisteEnBaseDeDatosYSePuedeRecuperarPorRepositorio() throws Exception {
        String payload = """
                {
                    "categoriaId": 3,
                    "titulo": "Revisión de caño de gas",
                    "descripcion": "Verificación periódica antes del invierno.",
                    "nivelUrgencia": "MEDIA"
                }
                """;

        MvcResult result = mockMvc.perform(post("/api/v1/solicitudes")
                        .header("Authorization", "Bearer " + tokenCliente1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        long id = json.get("id").asLong();

        Optional<Solicitud> persisted = solicitudRepository.findById(id);
        assertTrue(persisted.isPresent(), "La solicitud debe existir en la base de datos");
        Solicitud s = persisted.get();
        assertEquals(3L, s.getCategoriaId());
        assertEquals("Revisión de caño de gas", s.getTitulo());
        assertEquals("Verificación periódica antes del invierno.", s.getDescripcion());
        assertEquals(NivelUrgencia.MEDIA, s.getNivelUrgencia());
        assertEquals("BORRADOR", s.getEstado());
        assertEquals(cliente1.getId(), s.getClienteId(), "Debe quedar asociada al cliente autenticado");
    }

    @Test
    void rechazarPeticionConCamposFaltantesOEnBlancoDevuelve400() throws Exception {
        // Título en blanco
        String tituloEnBlanco = """
                {
                    "categoriaId": 1,
                    "titulo": "   ",
                    "descripcion": "Descripción válida",
                    "nivelUrgencia": "MEDIA"
                }
                """;

        mockMvc.perform(post("/api/v1/solicitudes")
                        .header("Authorization", "Bearer " + tokenCliente1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(tituloEnBlanco))
                .andExpect(status().isBadRequest());

        // Descripción en blanco
        String descripcionEnBlanco = """
                {
                    "categoriaId": 1,
                    "titulo": "   ",
                    "descripcion": "   ",
                    "nivelUrgencia": "MEDIA"
                }
                """;

        mockMvc.perform(post("/api/v1/solicitudes")
                        .header("Authorization", "Bearer " + tokenCliente1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(descripcionEnBlanco))
                .andExpect(status().isBadRequest());

        // CategoriaId nulo
        String sinCategoria = """
                {
                    "titulo": "Título válido",
                    "descripcion": "Descripción válida",
                    "nivelUrgencia": "MEDIA"
                }
                """;

        mockMvc.perform(post("/api/v1/solicitudes")
                        .header("Authorization", "Bearer " + tokenCliente1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(sinCategoria))
                .andExpect(status().isBadRequest());

        // NivelUrgencia nulo
        String sinUrgencia = """
                {
                    "categoriaId": 1,
                    "titulo": "Título válido",
                    "descripcion": "Descripción válida"
                }
                """;

        mockMvc.perform(post("/api/v1/solicitudes")
                        .header("Authorization", "Bearer " + tokenCliente1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(sinUrgencia))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rechazarPeticionConTituloMayorA150CaracteresDevuelve400() throws Exception {
        String tituloLargo = "a".repeat(151);
        String payload = """
                {
                    "categoriaId": 1,
                    "titulo": "%s",
                    "descripcion": "Descripción válida",
                    "nivelUrgencia": "MEDIA"
                }
                """.formatted(tituloLargo);

        mockMvc.perform(post("/api/v1/solicitudes")
                        .header("Authorization", "Bearer " + tokenCliente1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rechazarPeticionConNivelUrgenciaInvalidoDevuelve400() throws Exception {
        String payload = """
                {
                    "categoriaId": 1,
                    "titulo": "Título válido",
                    "descripcion": "Descripción válida",
                    "nivelUrgencia": "SUPER_URGENTE"
                }
                """;

        mockMvc.perform(post("/api/v1/solicitudes")
                        .header("Authorization", "Bearer " + tokenCliente1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rechazarPeticionConCategoriaInexistenteDevuelve400() throws Exception {
        String payload = """
                {
                    "categoriaId": 999,
                    "titulo": "Título válido",
                    "descripcion": "Descripción válida",
                    "nivelUrgencia": "ALTA"
                }
                """;

        mockMvc.perform(post("/api/v1/solicitudes")
                        .header("Authorization", "Bearer " + tokenCliente1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest());
    }

    @Test
    void catalogoDeCategoriasSigueRespondiendoCorrectamenteSinAutenticacion() throws Exception {
        mockMvc.perform(get("/api/v1/categorias"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }

    @Test
    void consultarEstadoSolicitudExistenteDevuelve200ConIdYEstadoSinExponerOtrosCampos() throws Exception {
        String payload = """
                {
                    "categoriaId": 1,
                    "titulo": "Fuga de gas en cocina",
                    "descripcion": "Olor a gas cerca de las hornallas.",
                    "nivelUrgencia": "EMERGENCIA"
                }
                """;

        MvcResult createResult = mockMvc.perform(post("/api/v1/solicitudes")
                        .header("Authorization", "Bearer " + tokenCliente1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode responseJson = objectMapper.readTree(createResult.getResponse().getContentAsString());
        long id = responseJson.get("id").asLong();

        mockMvc.perform(get("/api/v1/solicitudes/{id}/estado", id)
                        .header("Authorization", "Bearer " + tokenCliente1))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.estado").value("BORRADOR"))
                .andExpect(jsonPath("$.titulo").doesNotExist())
                .andExpect(jsonPath("$.descripcion").doesNotExist())
                .andExpect(jsonPath("$.categoriaId").doesNotExist())
                .andExpect(jsonPath("$.nivelUrgencia").doesNotExist())
                .andExpect(jsonPath("$.clienteId").doesNotExist());
    }

    @Test
    void consultarEstadoDeSolicitudPersistidaPreviamenteDevuelve200() throws Exception {
        Solicitud guardada = solicitudRepository.save(new Solicitud(
                4L,
                "Cambio de cerradura",
                "Cerradura trabada de puerta principal.",
                NivelUrgencia.ALTA,
                "BORRADOR",
                cliente1.getId()
        ));

        mockMvc.perform(get("/api/v1/solicitudes/{id}/estado", guardada.getId())
                        .header("Authorization", "Bearer " + tokenCliente1))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(guardada.getId()))
                .andExpect(jsonPath("$.estado").value("BORRADOR"));
    }

    @Test
    void consultarEstadoSolicitudInexistenteDevuelve404() throws Exception {
        mockMvc.perform(get("/api/v1/solicitudes/{id}/estado", 99999)
                        .header("Authorization", "Bearer " + tokenCliente1))
                .andExpect(status().isNotFound());
    }

    // --- Pruebas específicas de autorización y propiedad B08 ---

    @Test
    void crearSolicitudConRolProfesionalDevuelve403() throws Exception {
        String payload = """
                {
                    "categoriaId": 1,
                    "titulo": "Intento de solicitud desde cuenta profesional",
                    "descripcion": "Descripción de prueba",
                    "nivelUrgencia": "MEDIA"
                }
                """;

        mockMvc.perform(post("/api/v1/solicitudes")
                        .header("Authorization", "Bearer " + tokenProfesional)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isForbidden());
    }

    @Test
    void crearSolicitudSinTokenDevuelve401() throws Exception {
        String payload = """
                {
                    "categoriaId": 1,
                    "titulo": "Intento anónimo",
                    "descripcion": "Descripción de prueba",
                    "nivelUrgencia": "MEDIA"
                }
                """;

        mockMvc.perform(post("/api/v1/solicitudes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void consultarEstadoSolicitudDeOtroClienteDevuelve404SinRevelarExistencia() throws Exception {
        // Cliente 1 crea el borrador
        Solicitud borradorCliente1 = solicitudRepository.save(new Solicitud(
                1L,
                "Borrador privado de cliente 1",
                "Detalles confidenciales",
                NivelUrgencia.BAJA,
                "BORRADOR",
                cliente1.getId()
        ));

        // Cliente 2 intenta consultar el estado del borrador de cliente 1
        mockMvc.perform(get("/api/v1/solicitudes/{id}/estado", borradorCliente1.getId())
                        .header("Authorization", "Bearer " + tokenCliente2))
                .andExpect(status().isNotFound());
    }

    @Test
    void consultarEstadoConRolProfesionalDevuelve403() throws Exception {
        Solicitud borrador = solicitudRepository.save(new Solicitud(
                1L,
                "Borrador existente",
                "Detalles",
                NivelUrgencia.BAJA,
                "BORRADOR",
                cliente1.getId()
        ));

        mockMvc.perform(get("/api/v1/solicitudes/{id}/estado", borrador.getId())
                        .header("Authorization", "Bearer " + tokenProfesional))
                .andExpect(status().isForbidden());
    }

    @Test
    void consultarEstadoSinTokenDevuelve401() throws Exception {
        mockMvc.perform(get("/api/v1/solicitudes/1/estado"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void consultarEstadoDeBorradorSinPropietarioCreadoAntesDeB08Devuelve404() throws Exception {
        // Simular borrador antiguo persistido antes de B08 (clienteId = null)
        Solicitud borradorHuerfano = solicitudRepository.save(new Solicitud(
                2L,
                "Borrador histórico anterior a B08",
                "Sin propietario asociado",
                NivelUrgencia.MEDIA,
                "BORRADOR",
                null
        ));

        mockMvc.perform(get("/api/v1/solicitudes/{id}/estado", borradorHuerfano.getId())
                        .header("Authorization", "Bearer " + tokenCliente1))
                .andExpect(status().isNotFound());
    }

    @Test
    void crearSolicitudAsociaAlClienteAutenticadoSinAceptarClienteIdEnElCuerpo() throws Exception {
        // Enviar payload intentando asignar a cliente 2
        String payload = """
                {
                    "categoriaId": 1,
                    "titulo": "Intento de spoofing de clienteId",
                    "descripcion": "Verificar que el backend no confie en campos de titularidad",
                    "nivelUrgencia": "BAJA",
                    "clienteId": %d
                }
                """.formatted(cliente2.getId());

        MvcResult result = mockMvc.perform(post("/api/v1/solicitudes")
                        .header("Authorization", "Bearer " + tokenCliente1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        long id = json.get("id").asLong();

        // En la base de datos debe pertenecer a cliente1 (el emisor del token), no a cliente2
        Solicitud guardada = solicitudRepository.findById(id).orElseThrow();
        assertEquals(cliente1.getId(), guardada.getClienteId());
    }
}
