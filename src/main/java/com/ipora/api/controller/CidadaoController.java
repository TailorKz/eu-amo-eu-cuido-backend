package com.ipora.api.controller;

import com.ipora.api.domain.Cidadao;
import com.ipora.api.repository.CidadaoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/cidadaos")
public class CidadaoController {

    @Autowired
    private CidadaoRepository repository;

    @PostMapping("/cadastrar")
    public ResponseEntity<Cidadao> cadastrar(@RequestBody Cidadao cidadao) {
        //  Verifica Telefone + Cidade
        if (repository.findByTelefoneAndCidade(cidadao.getTelefone(), cidadao.getCidade()).isPresent()) {
            return ResponseEntity.badRequest().build();
        }

        Cidadao salvo = repository.save(cidadao);
        return ResponseEntity.ok(salvo);
    }

    @PostMapping("/login")
    public ResponseEntity<Cidadao> login(@RequestBody Cidadao dadosLogin) {
        //  Busca pelo Telefone + Cidade que vieram do aplicativo
        var cidadaoOpt = repository.findByTelefoneAndCidade(dadosLogin.getTelefone(), dadosLogin.getCidade());

        if (cidadaoOpt.isPresent()) {
            Cidadao cidadao = cidadaoOpt.get();
            if (cidadao.getSenha().equals(dadosLogin.getSenha())) {
                return ResponseEntity.ok(cidadao);
            }
        }
        return ResponseEntity.status(401).build();
    }

    @GetMapping("/todos")
    public ResponseEntity<List<Cidadao>> listarTodos() {
        return ResponseEntity.ok(repository.findAll());
    }

    // 🔴 Rota para o Painel Web (Super Admin) - Promover/Alterar o cargo de um usuário
    @PutMapping("/{id}/perfil")
    public ResponseEntity<Cidadao> atualizarPerfil(
            @PathVariable Long id,
            @RequestBody Cidadao dadosAtualizados) {

        // 🔴 CORRIGIDO AQUI: Usando apenas "repository"
        var cidadaoOpt = repository.findById(id);

        if(cidadaoOpt.isPresent()){
            Cidadao cidadao = cidadaoOpt.get();
            // Atualiza apenas o nível de acesso e o setor da pessoa
            cidadao.setPerfil(dadosAtualizados.getPerfil());
            cidadao.setSetorAtuacao(dadosAtualizados.getSetorAtuacao());

            // 🔴 CORRIGIDO AQUI: Usando apenas "repository"
            return ResponseEntity.ok(repository.save(cidadao));
        }
        return ResponseEntity.notFound().build();
    }
}