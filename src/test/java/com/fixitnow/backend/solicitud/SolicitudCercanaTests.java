package com.fixitnow.backend.solicitud;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fixitnow.backend.direccion.DireccionRepository;
import com.fixitnow.backend.profesional.PerfilProfesionalRepository;
import com.fixitnow.backend.security.JwtService;
import com.fixitnow.backend.usuario.RolUsuario;
import com.fixitnow.backend.usuario.Usuario;
import com.fixitnow.backend.usuario.UsuarioRepository;

@SpringBootTest
@AutoConfigureMockMvc
class SolicitudCercanaTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SolicitudRepository solicitudRepository;

    @Autowired
    private DireccionRepository direccionRepository;

    @Autowired
    private PerfilProfesionalRepository perfilRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    private Usuario cliente;
    private Usuario profesional;
    private Usuario profesionalSinPerfil;
    private String tokenCliente;
    private String tokenProfesional;
    private String tokenProfesionalSinPerfil;

    @BeforeEach
    void setUp() {
        solicitudRepository.deleteAll();
        direccionRepository.deleteAll();
        perfilRepository.deleteAll();
        usuarioRepository.deleteAll();

        cliente = usuarioRepository.save(new Usuario(
                "cliente.cercanas@ejemplo.com",
                passwordEncoder.encode("claveSegura123"),
                RolUsuario.CLIENTE
        ));
        profesional = usuarioRepository.save(new Usuario(
                "profesional.cercanas@ejemplo.com",
                passwordEncoder.encode("claveSegura123"),
                RolUsuario.PROFESIONAL
        ));
        profesionalSinPerfil = usuarioRepository.save(new Usuario(
                "prof.sinperfil@ejemplo.com",
                passwordEncoder.encode("claveSegura123"),
                RolUsuario.PROFESIONAL
        ));

        tokenCliente = jwtService.generarToken(cliente);
        tokenProfesional = jwtService.generarToken(profesional);
        tokenProfesionalSinPerfil = jwtService.generarToken(profesionalSinPerfil);
    }

    private long crearDireccionCliente(String calle, double lat, double lon) throws Exception {
        String payload = """
                {
                    "calle": "%s",
                    "numero": "100",
                    "ciudad": "Buenos Aires",
                    "latitud": %f,
                    "longitud": %f
                }
                """.formatted(calle, lat, lon);

        MvcResult result = mockMvc.perform(post("/api/v1/direcciones")
                        .header("Authorization", "Bearer " + tokenCliente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return json.get("id").asLong();
    }

    private long crearBorrador(long categoriaId, String titulo, String urgencia) throws Exception {
        String payload = """
                {
                    "categoriaId": %d,
                    "titulo": "%s",
                    "descripcion": "Descripción detallada del problema doméstico.",
                    "nivelUrgencia": "%s"
                }
                """.formatted(categoriaId, titulo, urgencia);

        MvcResult result = mockMvc.perform(post("/api/v1/solicitudes")
                        .header("Authorization", "Bearer " + tokenCliente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return json.get("id").asLong();
    }

    private void publicarSolicitud(long solicitudId, long direccionId) throws Exception {
        String payload = """
                {
                    "direccionId": %d,
                    "fechaTentativa": "2026-11-25T14:30:00"
                }
                """.formatted(direccionId);

        mockMvc.perform(patch("/api/v1/solicitudes/" + solicitudId + "/publicacion")
                        .header("Authorization", "Bearer " + tokenCliente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk());
    }

    private void configurarPerfilProfesional(String token, String categorias, double lat, double lon, int radioKm) throws Exception {
        String payload = """
                {
                    "categoriaIds": [%s],
                    "matriculaLicencia": "MAT-001",
                    "descripcionExperiencia": "Profesional matriculado con experiencia comprobable.",
                    "latitudBase": %f,
                    "longitudBase": %f,
                    "radioCoberturaKm": %d
                }
                """.formatted(categorias, lat, lon, radioKm);

        mockMvc.perform(put("/api/v1/profesionales/perfil")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk());
    }

    @Test
    void profesionalObtieneSolicitudPublicadaEnSuRadioYCategoriaConDatosPublicosYDistancia() throws Exception {
        // Base profesional en Obelisco (-34.6037, -58.3816), radio 15 km, categorías Plomería (1) y Electricidad (2)
        configurarPerfilProfesional(tokenProfesional, "1, 2", -34.6037, -58.3816, 15);

        // Cliente crea dirección a ~2 km en Recoleta (-34.5880, -58.3970)
        long dirId = crearDireccionCliente("Av. Santa Fe", -34.5880, -58.3970);
        long solId = crearBorrador(1, "Reparar caño con pérdida en cocina", "ALTA");
        publicarSolicitud(solId, dirId);

        mockMvc.perform(get("/api/v1/solicitudes/cercanas")
                        .header("Authorization", "Bearer " + tokenProfesional))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(solId))
                .andExpect(jsonPath("$[0].categoriaId").value(1))
                .andExpect(jsonPath("$[0].categoriaNombre").value("Plomería"))
                .andExpect(jsonPath("$[0].titulo").value("Reparar caño con pérdida en cocina"))
                .andExpect(jsonPath("$[0].descripcion").value("Descripción detallada del problema doméstico."))
                .andExpect(jsonPath("$[0].nivelUrgencia").value("ALTA"))
                .andExpect(jsonPath("$[0].fechaTentativa").value("2026-11-25T14:30:00"))
                .andExpect(jsonPath("$[0].distanciaKm", notNullValue()))
                // Asegurar que no se filtren datos personales del cliente ni la dirección exacta
                .andExpect(jsonPath("$[0].clienteId").doesNotExist())
                .andExpect(jsonPath("$[0].clienteEmail").doesNotExist())
                .andExpect(jsonPath("$[0].direccionId").doesNotExist())
                .andExpect(jsonPath("$[0].calle").doesNotExist())
                .andExpect(jsonPath("$[0].numero").doesNotExist())
                .andExpect(jsonPath("$[0].pisoDepto").doesNotExist())
                .andExpect(jsonPath("$[0].codigoPostal").doesNotExist());
    }

    @Test
    void solicitudFueraDelRadioDeCoberturaEsExcluida() throws Exception {
        // Base profesional en Obelisco (-34.6037, -58.3816), radio 10 km
        configurarPerfilProfesional(tokenProfesional, "1", -34.6037, -58.3816, 10);

        // Dirección en La Plata (-34.9214, -57.9545, aprox ~53.6 km de distancia)
        long dirId = crearDireccionCliente("Calle 7", -34.9214, -57.9545);
        long solId = crearBorrador(1, "Plomería en La Plata", "MEDIA");
        publicarSolicitud(solId, dirId);

        mockMvc.perform(get("/api/v1/solicitudes/cercanas")
                        .header("Authorization", "Bearer " + tokenProfesional))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    void solicitudDeOtraCategoriaEsExcluida() throws Exception {
        // Profesional solo atiende categoría 1 (Plomería)
        configurarPerfilProfesional(tokenProfesional, "1", -34.6037, -58.3816, 15);

        // Solicitud en la misma esquina del profesional pero categoría 3 (Gas)
        long dirId = crearDireccionCliente("Av. Corrientes", -34.6037, -58.3816);
        long solId = crearBorrador(3, "Instalación de calefactor a gas", "BAJA");
        publicarSolicitud(solId, dirId);

        mockMvc.perform(get("/api/v1/solicitudes/cercanas")
                        .header("Authorization", "Bearer " + tokenProfesional))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    void solicitudEnEstadoBorradorEsExcluida() throws Exception {
        configurarPerfilProfesional(tokenProfesional, "1", -34.6037, -58.3816, 15);

        // Se crea el borrador con categoría 1, pero NO se publica
        crearBorrador(1, "Borrador de plomería no publicado", "BAJA");

        mockMvc.perform(get("/api/v1/solicitudes/cercanas")
                        .header("Authorization", "Bearer " + tokenProfesional))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    void profesionalSinPerfilConfiguradoRecibe409Conflict() throws Exception {
        mockMvc.perform(get("/api/v1/solicitudes/cercanas")
                        .header("Authorization", "Bearer " + tokenProfesionalSinPerfil))
                .andExpect(status().isConflict());
    }

    @Test
    void clienteIntentandoAccederASolicitudesCercanasRecibe403() throws Exception {
        mockMvc.perform(get("/api/v1/solicitudes/cercanas")
                        .header("Authorization", "Bearer " + tokenCliente))
                .andExpect(status().isForbidden());
    }

    @Test
    void peticionSinTokenRecibe401() throws Exception {
        mockMvc.perform(get("/api/v1/solicitudes/cercanas"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void multiplesSolicitudesFiltradasCorrectamenteSegunRadioYCategoria() throws Exception {
        // Profesional en Buenos Aires con radio 20 km y especialidades 1 (Plomería) y 2 (Electricidad)
        configurarPerfilProfesional(tokenProfesional, "1, 2", -34.6037, -58.3816, 20);

        // Dirección A: cercana (a ~3 km)
        long dirCercana = crearDireccionCliente("Palermo", -34.5800, -58.4200);
        // Dirección B: lejana (La Plata, ~53 km)
        long dirLejana = crearDireccionCliente("La Plata", -34.9214, -57.9545);

        // Solicitud 1: Cat 1 (Plomería), cercana -> DEBE ENTRAR
        long s1 = crearBorrador(1, "Sol 1 Plomería Cercana", "ALTA");
        publicarSolicitud(s1, dirCercana);

        // Solicitud 2: Cat 2 (Electricidad), cercana -> DEBE ENTRAR
        long s2 = crearBorrador(2, "Sol 2 Electricidad Cercana", "MEDIA");
        publicarSolicitud(s2, dirCercana);

        // Solicitud 3: Cat 1 (Plomería), lejana -> EXCLUIDA POR RADIO
        long s3 = crearBorrador(1, "Sol 3 Plomería Lejana", "BAJA");
        publicarSolicitud(s3, dirLejana);

        // Solicitud 4: Cat 4 (Cerrajería), cercana -> EXCLUIDA POR CATEGORÍA
        long s4 = crearBorrador(4, "Sol 4 Cerrajería Cercana", "ALTA");
        publicarSolicitud(s4, dirCercana);

        // Solicitud 5: Cat 1 (Plomería), cercana, pero en BORRADOR -> EXCLUIDA POR ESTADO
        crearBorrador(1, "Sol 5 Borrador Cercano", "ALTA");

        mockMvc.perform(get("/api/v1/solicitudes/cercanas")
                        .header("Authorization", "Bearer " + tokenProfesional))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[?(@.id == " + s1 + ")].titulo").value("Sol 1 Plomería Cercana"))
                .andExpect(jsonPath("$[?(@.id == " + s2 + ")].titulo").value("Sol 2 Electricidad Cercana"));
    }
}
