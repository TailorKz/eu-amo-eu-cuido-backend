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

    //  Rota para o Painel Web (Super Admin) - Promover/Alterar o cargo de um usuário
    @PutMapping("/{id}/perfil")
    public ResponseEntity<Cidadao> atualizarPerfil(
            @PathVariable Long id,
            @RequestBody Cidadao dadosAtualizados) {

        // Usando apenas "repository"
        var cidadaoOpt = repository.findById(id);

        if(cidadaoOpt.isPresent()){
            Cidadao cidadao = cidadaoOpt.get();
            // Atualiza apenas o nível de acesso e o setor da pessoa
            cidadao.setPerfil(dadosAtualizados.getPerfil());
            cidadao.setSetorAtuacao(dadosAtualizados.getSetorAtuacao());

            // Usando apenas "repository"
            return ResponseEntity.ok(repository.save(cidadao));
        }
        return ResponseEntity.notFound().build();
    }
    // ==========================================
    //  ROTAS DE SEGURANÇA (PERFIL)
    // ==========================================

    // 1. GERA CÓDIGO E ENVIA WHATSAPP
    @PostMapping("/{id}/solicitar-codigo")
    public ResponseEntity<Void> solicitarCodigoVerificacao(
            @PathVariable Long id,
            @RequestParam String tipo, // "SENHA" ou "NUMERO"
            @RequestParam(required = false) String novoNumero) {

        var cidadaoOpt = repository.findById(id);
        if(cidadaoOpt.isEmpty()) return ResponseEntity.notFound().build();

        Cidadao cidadao = cidadaoOpt.get();

        // Gera um código de 4 dígitos aleatório (ex: 4092)
        String codigoGerado = String.format("%04d", new java.util.Random().nextInt(10000));
        cidadao.setCodigoVerificacao(codigoGerado);
        repository.save(cidadao);

        // AQUI ENTRARÁ O CÓDIGO DO TWILIO NO FUTURO
        String numeroDestino = tipo.equals("NUMERO") ? novoNumero : cidadao.getTelefone();
        System.out.println("Enviando WhatsApp para " + numeroDestino + " - Código: " + codigoGerado);

        return ResponseEntity.ok().build();
    }

    // 2. ALTERAR A SENHA (Recebe o código + nova senha)
    @PutMapping("/{id}/alterar-senha")
    public ResponseEntity<String> alterarSenha(
            @PathVariable Long id,
            @RequestParam String codigo,
            @RequestParam String novaSenha) {

        var cidadaoOpt = repository.findById(id);
        if(cidadaoOpt.isEmpty()) return ResponseEntity.notFound().build();
        Cidadao cidadao = cidadaoOpt.get();

        if (cidadao.getCodigoVerificacao() != null && cidadao.getCodigoVerificacao().equals(codigo)) {
            cidadao.setSenha(novaSenha);
            cidadao.setCodigoVerificacao(null); // Limpa o código depois de usar
            repository.save(cidadao);
            return ResponseEntity.ok("Senha alterada com sucesso.");
        }

        return ResponseEntity.badRequest().body("Código inválido.");
    }

    // 3. VERIFICAR A SENHA ATUAL (Antes de deixar trocar o número)
    @PostMapping("/{id}/verificar-senha")
    public ResponseEntity<Boolean> verificarSenhaAtual(
            @PathVariable Long id,
            @RequestBody String senhaDigitada) {

        var cidadaoOpt = repository.findById(id);
        if(cidadaoOpt.isPresent() && cidadaoOpt.get().getSenha().equals(senhaDigitada)) {
            return ResponseEntity.ok(true);
        }
        return ResponseEntity.status(401).body(false);
    }

    // 4. ALTERAR O NÚMERO (Recebe o código + o novo número)
    @PutMapping("/{id}/alterar-numero")
    public ResponseEntity<Cidadao> alterarNumero(
            @PathVariable Long id,
            @RequestParam String codigo,
            @RequestParam String novoNumero) {

        var cidadaoOpt = repository.findById(id);
        if(cidadaoOpt.isEmpty()) return ResponseEntity.notFound().build();
        Cidadao cidadao = cidadaoOpt.get();

        if (cidadao.getCodigoVerificacao() != null && cidadao.getCodigoVerificacao().equals(codigo)) {
            cidadao.setTelefone(novoNumero);
            cidadao.setCodigoVerificacao(null);

            // Devolve o cidadão atualizado para o Celular salvar no useAuthStore
            return ResponseEntity.ok(repository.save(cidadao));
        }

        return ResponseEntity.badRequest().build();
    }
}