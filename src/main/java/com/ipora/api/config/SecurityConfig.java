package com.ipora.api.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    @Autowired
    private SecurityFilter securityFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(org.springframework.security.config.Customizer.withDefaults())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(org.springframework.http.HttpMethod.OPTIONS, "/**").permitAll()

                        // Rotas permitidas
                        .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/cidadaos/login").permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/cidadaos").permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/cidadaos/migrar-senhas").permitAll()

                        // ROTAS (Cadastro e Esqueci a Senha)
                        .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/cidadaos/cadastrar").permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/cidadaos/enviar-otp-cadastro").permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/cidadaos/recuperar-senha/solicitar").permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/cidadaos/recuperar-senha/validar-codigo").permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.PUT, "/api/cidadaos/recuperar-senha/alterar").permitAll()

                        .anyRequest().authenticated()
                )
                .addFilterBefore(securityFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}