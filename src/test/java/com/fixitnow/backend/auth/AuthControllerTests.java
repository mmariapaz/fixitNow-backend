package com.fixitnow.backend.auth;

import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.time.Instant;

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
class AuthControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    private Usuario usuarioPrueba;

    @BeforeEach
    void setUp() {
        usuarioRepository.deleteAll();
        usuarioPrueba = usuarioRepository.save(new Usuario(
                "cliente.login@ejemplo.com",
                passwordEncoder.encode("claveSegura123"),
                RolUsuario.CLIENTE
        ));
    }

    @Test
    void loginConCredencialesValidasDevuelve200ConAccessTokenTipoBearerYExpiracion() throws Exception {
        String payload = """
                {
                    "correo": "cliente.login@ejemplo.com",
                    "contrasena": "claveSegura123"
                }
                """;

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.accessToken", notNullValue()))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(900));
    }

    @Test
    void loginConCredencialesValidasNormalizaEspaciosYMayusculas() throws Exception {
        String payload = """
                {
                    "correo": "   CLIENTE.LOGIN@EJEMPLO.COM  ",
                    "contrasena": "claveSegura123"
                }
                """;

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken", notNullValue()))
                .andExpect(jsonPath("$.tokenType").value("Bearer"));
    }

    @Test
    void loginConContrasenaIncorrectaDevuelve401ConMensajeGenerico() throws Exception {
        String payload = """
                {
                    "correo": "cliente.login@ejemplo.com",
                    "contrasena": "claveEquivocada999"
                }
                """;

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginConCorreoDesconocidoDevuelve401ConMensajeGenericoSinRevelarExistencia() throws Exception {
        String payload = """
                {
                    "correo": "fantasma@ejemplo.com",
                    "contrasena": "claveSegura123"
                }
                """;

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void consultarMeConTokenValidoDevuelve200ConDatosDeUsuario() throws Exception {
        String token = jwtService.generarToken(usuarioPrueba);

        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(usuarioPrueba.getId()))
                .andExpect(jsonPath("$.correo").value("cliente.login@ejemplo.com"))
                .andExpect(jsonPath("$.rol").value("CLIENTE"))
                .andExpect(jsonPath("$.contrasena").doesNotExist())
                .andExpect(jsonPath("$.contrasenaHash").doesNotExist());
    }

    @Test
    void consultarMeSinCabeceraAuthorizationDevuelve401() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void consultarMeConTokenAlteradoDevuelve401() throws Exception {
        String tokenValido = jwtService.generarToken(usuarioPrueba);
        String tokenAlterado = tokenValido.substring(0, tokenValido.length() - 8) + "abcdefgh";

        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + tokenAlterado))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void consultarMeConTokenVencidoDevuelve401SinEsperar15Minutos() throws Exception {
        // Token emitido hace 20 minutos con duración de 5 minutos (expiró hace 15 minutos)
        Instant emitidoEn = Instant.now().minus(Duration.ofMinutes(20));
        Duration duracion = Duration.ofMinutes(5);
        String tokenVencido = jwtService.generarTokenConTiempos(usuarioPrueba, emitidoEn, duracion);

        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + tokenVencido))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rutasAnterioresSiguenOperativasYCompatibles() throws Exception {
        mockMvc.perform(get("/api/v1/categorias"))
                .andExpect(status().isOk());

        String solicitudPayload = """
                {
                    "categoriaId": 1,
                    "titulo": "Prueba de compatibilidad de solicitud",
                    "descripcion": "Verificacion de funcionamiento con cliente autenticado",
                    "nivelUrgencia": "BAJA"
                }
                """;

        String token = jwtService.generarToken(usuarioPrueba);
        mockMvc.perform(post("/api/v1/solicitudes")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(solicitudPayload))
                .andExpect(status().isCreated());
    }
}
