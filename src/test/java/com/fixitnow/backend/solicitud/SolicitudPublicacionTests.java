package com.fixitnow.backend.solicitud;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import com.fixitnow.backend.direccion.Direccion;
import com.fixitnow.backend.direccion.DireccionRepository;
import com.fixitnow.backend.security.JwtService;
import com.fixitnow.backend.usuario.RolUsuario;
import com.fixitnow.backend.usuario.Usuario;
import com.fixitnow.backend.usuario.UsuarioRepository;

@SpringBootTest
@AutoConfigureMockMvc
class SolicitudPublicacionTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SolicitudRepository solicitudRepository;

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

    private Direccion direccionCliente1;
    private Direccion direccionCliente2;
    private Solicitud borradorCliente1;

    @BeforeEach
    void setUp() {
        solicitudRepository.deleteAll();
        direccionRepository.deleteAll();
        usuarioRepository.deleteAll();

        cliente1 = usuarioRepository.save(new Usuario(
                "cliente1.publicar@ejemplo.com",
                passwordEncoder.encode("claveSegura123"),
                RolUsuario.CLIENTE
        ));
        cliente2 = usuarioRepository.save(new Usuario(
                "cliente2.publicar@ejemplo.com",
                passwordEncoder.encode("claveSegura123"),
                RolUsuario.CLIENTE
        ));
        profesional = usuarioRepository.save(new Usuario(
                "profesional.publicar@ejemplo.com",
                passwordEncoder.encode("claveSegura123"),
                RolUsuario.PROFESIONAL
        ));

        tokenCliente1 = jwtService.generarToken(cliente1);
        tokenCliente2 = jwtService.generarToken(cliente2);
        tokenProfesional = jwtService.generarToken(profesional);

        direccionCliente1 = direccionRepository.save(new Direccion(
                cliente1.getId(),
                "San Martín",
                "450",
                null,
                "Quilmes",
                "B1878",
                -34.7242,
                -58.2608
        ));

        direccionCliente2 = direccionRepository.save(new Direccion(
                cliente2.getId(),
                "Mitre",
                "800",
                null,
                "Avellaneda",
                "B1870",
                -34.6611,
                -58.3653
        ));

        borradorCliente1 = solicitudRepository.save(new Solicitud(
                1L,
                "Reparación de calefón",
                "No enciende el piloto al abrir el agua.",
                NivelUrgencia.ALTA,
                "BORRADOR",
                cliente1.getId()
        ));
    }

    @Test
    void publicarBorradorPropioConDireccionPropiaDevuelve200ConEstadoPublicada() throws Exception {
        String payload = """
                {
                    "direccionId": %d,
                    "fechaTentativa": "2026-11-20T10:30:00"
                }
                """.formatted(direccionCliente1.getId());

        mockMvc.perform(patch("/api/v1/solicitudes/{id}/publicacion", borradorCliente1.getId())
                        .header("Authorization", "Bearer " + tokenCliente1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(borradorCliente1.getId()))
                .andExpect(jsonPath("$.estado").value("PUBLICADA"))
                .andExpect(jsonPath("$.direccionId").value(direccionCliente1.getId()))
                .andExpect(jsonPath("$.fechaTentativa").value("2026-11-20T10:30:00"))
                .andExpect(jsonPath("$.nivelUrgencia").value("ALTA"))
                .andExpect(jsonPath("$.calle").doesNotExist())
                .andExpect(jsonPath("$.numero").doesNotExist())
                .andExpect(jsonPath("$.clienteId").doesNotExist());

        // Comprobar persistencia real atómica en BD
        Solicitud actualizada = solicitudRepository.findById(borradorCliente1.getId()).orElseThrow();
        assertEquals("PUBLICADA", actualizada.getEstado());
        assertEquals(direccionCliente1.getId(), actualizada.getDireccionId());
        assertEquals(LocalDateTime.parse("2026-11-20T10:30:00"), actualizada.getFechaTentativa());
    }

    @Test
    void publicarBorradorAjenoDevuelve404SinModificarNada() throws Exception {
        String payload = """
                {
                    "direccionId": %d,
                    "fechaTentativa": "2026-11-20T10:30:00"
                }
                """.formatted(direccionCliente2.getId());

        // Cliente 2 intenta publicar el borrador de Cliente 1
        mockMvc.perform(patch("/api/v1/solicitudes/{id}/publicacion", borradorCliente1.getId())
                        .header("Authorization", "Bearer " + tokenCliente2)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isNotFound());

        // El borrador original no debe haber cambiado
        Solicitud sinModificar = solicitudRepository.findById(borradorCliente1.getId()).orElseThrow();
        assertEquals("BORRADOR", sinModificar.getEstado());
    }

    @Test
    void publicarBorradorPropioConDireccionDeOtroClienteDevuelve404SinModificarBorrador() throws Exception {
        // Cliente 1 intenta asociar la dirección de Cliente 2
        String payload = """
                {
                    "direccionId": %d,
                    "fechaTentativa": "2026-11-20T10:30:00"
                }
                """.formatted(direccionCliente2.getId());

        mockMvc.perform(patch("/api/v1/solicitudes/{id}/publicacion", borradorCliente1.getId())
                        .header("Authorization", "Bearer " + tokenCliente1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isNotFound());

        // El borrador debe seguir en BORRADOR y sin dirección asociada
        Solicitud sinModificar = solicitudRepository.findById(borradorCliente1.getId()).orElseThrow();
        assertEquals("BORRADOR", sinModificar.getEstado());
        assertEquals(null, sinModificar.getDireccionId());
    }

    @Test
    void segundoIntentoDePublicarMismoBorradorDevuelve409SinCambiarDatos() throws Exception {
        String payload = """
                {
                    "direccionId": %d,
                    "fechaTentativa": "2026-11-20T10:30:00"
                }
                """.formatted(direccionCliente1.getId());

        // Primera publicación exitosa
        mockMvc.perform(patch("/api/v1/solicitudes/{id}/publicacion", borradorCliente1.getId())
                        .header("Authorization", "Bearer " + tokenCliente1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk());

        // Segundo intento de publicación
        mockMvc.perform(patch("/api/v1/solicitudes/{id}/publicacion", borradorCliente1.getId())
                        .header("Authorization", "Bearer " + tokenCliente1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isConflict());
    }

    @Test
    void publicarConRolProfesionalDevuelve403() throws Exception {
        String payload = """
                {
                    "direccionId": %d,
                    "fechaTentativa": "2026-11-20T10:30:00"
                }
                """.formatted(direccionCliente1.getId());

        mockMvc.perform(patch("/api/v1/solicitudes/{id}/publicacion", borradorCliente1.getId())
                        .header("Authorization", "Bearer " + tokenProfesional)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isForbidden());
    }

    @Test
    void publicarSinTokenDevuelve401() throws Exception {
        String payload = """
                {
                    "direccionId": %d,
                    "fechaTentativa": "2026-11-20T10:30:00"
                }
                """.formatted(direccionCliente1.getId());

        mockMvc.perform(patch("/api/v1/solicitudes/{id}/publicacion", borradorCliente1.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void publicarConCamposFaltantesDevuelve400() throws Exception {
        String sinDireccion = """
                {
                    "fechaTentativa": "2026-11-20T10:30:00"
                }
                """;

        mockMvc.perform(patch("/api/v1/solicitudes/{id}/publicacion", borradorCliente1.getId())
                        .header("Authorization", "Bearer " + tokenCliente1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(sinDireccion))
                .andExpect(status().isBadRequest());

        String sinFecha = """
                {
                    "direccionId": %d
                }
                """.formatted(direccionCliente1.getId());

        mockMvc.perform(patch("/api/v1/solicitudes/{id}/publicacion", borradorCliente1.getId())
                        .header("Authorization", "Bearer " + tokenCliente1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(sinFecha))
                .andExpect(status().isBadRequest());
    }
}
