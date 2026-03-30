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
}