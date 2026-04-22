package com.ipora.api.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.ipora.api.domain.Cidadao;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Service
public class TokenService {

    // chave secreta deve ficar no application.properties futuramente
    @Value("${api.security.token.secret:minha-chave-secreta-govtech}")
    private String secret;

    public String gerarToken(Cidadao cidadao) {
        try {
            Algorithm algoritmo = Algorithm.HMAC256(secret);
            return JWT.create()
                    .withIssuer("eu-amo-eu-cuido-api")
                    .withSubject(cidadao.getTelefone())
                    .withClaim("id", cidadao.getId())
                    .withClaim("perfil", cidadao.getPerfil() != null ? cidadao.getPerfil() : "CIDADAO")
                    .withClaim("cidade", cidadao.getCidade() != null ? cidadao.getCidade() : "")
                    .withExpiresAt(dataExpiracao())
                    .sign(algoritmo);
        } catch (JWTCreationException exception) {
            throw new RuntimeException("Erro ao gerar token JWT", exception);
        }
    }

    private Instant dataExpiracao() {
        return LocalDateTime.now().plusDays(3650).toInstant(ZoneOffset.of("-03:00"));
    }

    public String getSubject(String tokenJWT) {
        try {
            com.auth0.jwt.algorithms.Algorithm algoritmo = com.auth0.jwt.algorithms.Algorithm.HMAC256(secret);
            return com.auth0.jwt.JWT.require(algoritmo)
                    .withIssuer("eu-amo-eu-cuido-api")
                    .build()
                    .verify(tokenJWT)
                    .getSubject(); // Devolve o número de telemóvel que guardámos no token
        } catch (com.auth0.jwt.exceptions.JWTVerificationException exception) {
            throw new RuntimeException("Token JWT inválido ou expirado!");
        }
    }

    public String getPerfil(String tokenJWT) {
        try {
            com.auth0.jwt.algorithms.Algorithm algoritmo = com.auth0.jwt.algorithms.Algorithm.HMAC256(secret);
            return com.auth0.jwt.JWT.require(algoritmo)
                    .withIssuer("eu-amo-eu-cuido-api")
                    .build()
                    .verify(tokenJWT)
                    .getClaim("perfil").asString();
        } catch (com.auth0.jwt.exceptions.JWTVerificationException exception) {
            return "CIDADAO"; // Em caso de erro ou token inválido, assume o menor privilégio
        }
    }
}