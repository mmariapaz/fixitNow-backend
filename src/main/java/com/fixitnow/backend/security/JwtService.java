package com.fixitnow.backend.security;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Service;

import com.fixitnow.backend.usuario.Usuario;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import jakarta.annotation.PostConstruct;

@Service
public class JwtService {

    public static final String ISSUER = "fixitnow-backend";
    public static final long EXPIRATION_SECONDS = 900; // 15 minutos

    @Value("${jwt.secret:}")
    private String jwtSecret;

    private SecretKey secretKey;

    @PostConstruct
    public void validarYConfigurarClave() {
        if (jwtSecret == null || jwtSecret.trim().isEmpty()) {
            throw new IllegalStateException("La variable de entorno JWT_SECRET es obligatoria y no puede estar vacía.");
        }
        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalStateException("JWT_SECRET debe tener una longitud mínima de 256 bits (32 bytes).");
        }
        this.secretKey = new SecretKeySpec(keyBytes, "HmacSHA256");
    }

    public String generarToken(Usuario usuario) {
        return generarTokenConTiempos(usuario, Instant.now(), Duration.ofSeconds(EXPIRATION_SECONDS));
    }

    public String generarTokenConTiempos(Usuario usuario, Instant emitidoEn, Duration duracion) {
        try {
            Instant expiraEn = emitidoEn.plus(duracion);
            JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                    .subject(String.valueOf(usuario.getId()))
                    .issuer(ISSUER)
                    .issueTime(Date.from(emitidoEn))
                    .expirationTime(Date.from(expiraEn))
                    .build();

            SignedJWT signedJWT = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claimsSet);
            signedJWT.sign(new MACSigner(secretKey));
            return signedJWT.serialize();
        } catch (Exception e) {
            throw new RuntimeException("Error al generar el token JWT", e);
        }
    }

    public JwtDecoder crearJwtDecoder() {
        if (this.secretKey == null) {
            validarYConfigurarClave();
        }
        return NimbusJwtDecoder.withSecretKey(this.secretKey).build();
    }
}
