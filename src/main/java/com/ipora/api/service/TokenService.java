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

    // O token expira em 30 dias (ideal para apps mobile para não deslogar a toda a hora)
    private Instant dataExpiracao() {
        return LocalDateTime.now().plusDays(30).toInstant(ZoneOffset.of("-03:00"));
    }
}