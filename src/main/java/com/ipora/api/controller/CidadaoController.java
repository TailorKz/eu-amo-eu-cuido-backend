package com.ipora.api.controller;

import com.ipora.api.domain.Cidadao;
import com.ipora.api.repository.CidadaoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cidadaos")
public class CidadaoController {

    @Autowired
    private CidadaoRepository repository;

    // Rota para Cadastrar um novo cidadão
    @PostMapping("/cadastrar")
    public ResponseEntity<Cidadao> cadastrar(@RequestBody Cidadao cidadao) {
        // Verifica se o telefone já existe
        if (repository.findByTelefone(cidadao.getTelefone()).isPresent()) {
            return ResponseEntity.badRequest().build(); // Retorna erro 400 se já existir
        }

        // Salva no banco de dados
        Cidadao salvo = repository.save(cidadao);
        return ResponseEntity.ok(salvo); // Retorna sucesso 200 e os dados do usuário
    }
    // Rota para Fazer Login
    @PostMapping("/login")
    public ResponseEntity<Cidadao> login(@RequestBody Cidadao dadosLogin) {

        // 1. Vai ao banco de dados procurar se existe alguém com este telefone
        var cidadaoOpt = repository.findByTelefone(dadosLogin.getTelefone());

        // 2. Se o cidadão existir...
        if (cidadaoOpt.isPresent()) {
            Cidadao cidadao = cidadaoOpt.get();

            // 3. Compara a senha que veio do telemóvel com a senha guardada no banco
            if (cidadao.getSenha().equals(dadosLogin.getSenha())) {
                // Senha certa! Retorna 200 OK e os dados do utilizador
                return ResponseEntity.ok(cidadao);
            }
        }

        // Se o telefone não existir OU a senha estiver errada, retorna erro 401 (Não Autorizado)
        return ResponseEntity.status(401).build();
    }
}