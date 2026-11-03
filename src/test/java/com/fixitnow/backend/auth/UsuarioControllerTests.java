package com.fixitnow.backend.auth;

import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Optional;

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
import com.fixitnow.backend.usuario.RolUsuario;
import com.fixitnow.backend.usuario.Usuario;
import com.fixitnow.backend.usuario.UsuarioRepository;

@SpringBootTest
@AutoConfigureMockMvc
class UsuarioControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void registrarCuentaClienteValidaDevuelve201ConIdCorreoYRolSinContrasenaNiHash() throws Exception {
        String payload = """
                {
                    "correo": "cliente.prueba1@ejemplo.com",
                    "contrasena": "claveSegura123",
                    "rol": "CLIENTE"
                }
                """;

        mockMvc.perform(post("/api/v1/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.correo").value("cliente.prueba1@ejemplo.com"))
                .andExpect(jsonPath("$.rol").value("CLIENTE"))
                .andExpect(jsonPath("$.contrasena").doesNotExist())
                .andExpect(jsonPath("$.contrasenaHash").doesNotExist())
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void registrarCuentaProfesionalValidaDevuelve201() throws Exception {
        String payload = """
                {
                    "correo": "profesional.prueba1@ejemplo.com",
                    "contrasena": "claveSegura123",
                    "rol": "PROFESIONAL"
                }
                """;

        mockMvc.perform(post("/api/v1/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.correo").value("profesional.prueba1@ejemplo.com"))
                .andExpect(jsonPath("$.rol").value("PROFESIONAL"));
    }

    @Test
    void rechazarPeticionConCorreoInvalidoOCamposFaltantesDevuelve400() throws Exception {
        // Correo inválido
        String correoInvalido = """
                {
                    "correo": "correo-no-valido",
                    "contrasena": "claveSegura123",
                    "rol": "CLIENTE"
                }
                """;

        mockMvc.perform(post("/api/v1/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(correoInvalido))
                .andExpect(status().isBadRequest());

        // Correo en blanco
        String correoEnBlanco = """
                {
                    "correo": "   ",
                    "contrasena": "claveSegura123",
                    "rol": "CLIENTE"
                }
                """;

        mockMvc.perform(post("/api/v1/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(correoEnBlanco))
                .andExpect(status().isBadRequest());

        // Contraseña en blanco
        String contrasenaEnBlanco = """
                {
                    "correo": "cliente2@ejemplo.com",
                    "contrasena": "   ",
                    "rol": "CLIENTE"
                }
                """;

        mockMvc.perform(post("/api/v1/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(contrasenaEnBlanco))
                .andExpect(status().isBadRequest());

        // Rol nulo
        String sinRol = """
                {
                    "correo": "cliente3@ejemplo.com",
                    "contrasena": "claveSegura123"
                }
                """;

        mockMvc.perform(post("/api/v1/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(sinRol))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rechazarPeticionConContrasenaMenorA8CaracteresDevuelve400() throws Exception {
        String claveCorta = """
                {
                    "correo": "cliente.corta@ejemplo.com",
                    "contrasena": "1234567",
                    "rol": "CLIENTE"
                }
                """;

        mockMvc.perform(post("/api/v1/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(claveCorta))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rechazarPeticionConRolInvalidoDevuelve400() throws Exception {
        String rolInvalido = """
                {
                    "correo": "admin@ejemplo.com",
                    "contrasena": "claveSegura123",
                    "rol": "ADMINISTRADOR"
                }
                """;

        mockMvc.perform(post("/api/v1/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(rolInvalido))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rechazarPeticionConCorreoDuplicadoDevuelve409() throws Exception {
        String payload = """
                {
                    "correo": "duplicado@ejemplo.com",
                    "contrasena": "claveSegura123",
                    "rol": "CLIENTE"
                }
                """;

        mockMvc.perform(post("/api/v1/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isConflict());
    }

    @Test
    void rechazarPeticionConCorreoDuplicadoVariandoMayusculasYEspaciosDevuelve409() throws Exception {
        String original = """
                {
                    "correo": "usuario.normalizar@ejemplo.com",
                    "contrasena": "claveSegura123",
                    "rol": "CLIENTE"
                }
                """;

        String variante = """
                {
                    "correo": "  USUARIO.NORMALIZAR@EJEMPLO.COM  ",
                    "contrasena": "claveSegura123",
                    "rol": "PROFESIONAL"
                }
                """;

        mockMvc.perform(post("/api/v1/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(original))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(variante))
                .andExpect(status().isConflict());
    }

    @Test
    void verificarQueContrasenaPersistidaEsHashDiferenteAlTextoPlanoYNoExpuestaEnRespuesta() throws Exception {
        String claveOriginal = "MiClaveSuperSegura2026!";
        String payload = """
                {
                    "correo": "verificar.hash@ejemplo.com",
                    "contrasena": "%s",
                    "rol": "CLIENTE"
                }
                """.formatted(claveOriginal);

        MvcResult result = mockMvc.perform(post("/api/v1/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        long id = json.get("id").asLong();

        // Verificar respuesta JSON
        assertTrue(json.get("contrasena") == null, "La respuesta no debe contener el campo contrasena");
        assertTrue(json.get("contrasenaHash") == null, "La respuesta no debe contener el campo contrasenaHash");

        // Verificar valor persistido en base de datos
        Optional<Usuario> usuarioOpt = usuarioRepository.findById(id);
        assertTrue(usuarioOpt.isPresent(), "El usuario debe existir en el repositorio");
        Usuario usuario = usuarioOpt.get();

        assertEquals("verificar.hash@ejemplo.com", usuario.getCorreo());
        assertEquals(RolUsuario.CLIENTE, usuario.getRol());
        assertNotEquals(claveOriginal, usuario.getContrasenaHash(), "La contraseña persistida no debe ser texto plano");
        assertTrue(passwordEncoder.matches(claveOriginal, usuario.getContrasenaHash()), "El hash persistido debe coincidir mediante PasswordEncoder");
    }
}
