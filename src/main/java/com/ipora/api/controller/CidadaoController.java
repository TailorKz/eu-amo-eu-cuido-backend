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

    //  INJETA O SERVIÇO DE SMS NO CONTROLLER
    @Autowired
    private com.ipora.api.service.SmsService smsService;

    // MÉTODO PARA GERAR O CÓDIGO ALEATÓRIO (Ex: 4589)
    private String gerarCodigoVerificacao() {
        return String.format("%04d", new java.util.Random().nextInt(10000));
    }

    //  ROTA NOVA: Gera OTP para Cadastro via SMS
    @PostMapping("/enviar-otp-cadastro")
    public ResponseEntity<java.util.Map<String, String>> enviarOtpCadastro(@RequestParam String telefone) {
        String codigo = gerarCodigoVerificacao();

        // Dispara o SMS
        smsService.enviarSms(telefone, codigo);

        // Devolve o código gerado para o App poder comparar com o que o usuário digitar
        java.util.Map<String, String> response = new java.util.HashMap<>();
        response.put("codigo", codigo);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/cadastrar")
    public ResponseEntity<Cidadao> cadastrar(@RequestBody Cidadao cidadao) {
        // Verifica Telefone + Cidade
        if (repository.findByTelefoneAndCidade(cidadao.getTelefone(), cidadao.getCidade()).isPresent()) {
            return ResponseEntity.badRequest().build();
        }

        Cidadao salvo = repository.save(cidadao);
        return ResponseEntity.ok(salvo);
    }

    @PostMapping("/login")
    public ResponseEntity<Cidadao> login(@RequestBody Cidadao dadosLogin) {
        // Busca pelo Telefone + Cidade que vieram do aplicativo
        var cidadaoOpt = repository.findByTelefoneAndCidade(dadosLogin.getTelefone(), dadosLogin.getCidade());

        if (cidadaoOpt.isPresent()) {
            Cidadao cidadao = cidadaoOpt.get();
            if (cidadao.getSenha().equals(dadosLogin.getSenha())) {
                return ResponseEntity.ok(cidadao);
            }
        }
        return ResponseEntity.status(401).build();
    }

    // o Admin só vê a cidade dele.
    @GetMapping("/cidade/{cidade}")
    public ResponseEntity<List<Cidadao>> listarPorCidade(@PathVariable String cidade) {
        return ResponseEntity.ok(repository.findByCidade(cidade));
    }

    // Rota para o Painel Web (Super Admin) - Promover/Alterar o cargo de um usuário
    @PutMapping("/{id}/perfil")
    public ResponseEntity<Cidadao> atualizarPerfil(
            @PathVariable Long id,
            @RequestBody Cidadao dadosAtualizados) {

        var cidadaoOpt = repository.findById(id);

        if(cidadaoOpt.isPresent()){
            Cidadao cidadao = cidadaoOpt.get();
            cidadao.setPerfil(dadosAtualizados.getPerfil());
            cidadao.setSetorAtuacao(dadosAtualizados.getSetorAtuacao());
            return ResponseEntity.ok(repository.save(cidadao));
        }
        return ResponseEntity.notFound().build();
    }

    // ==========================================
    // ROTAS DE SEGURANÇA (PERFIL DO APP LOGADO)
    // ==========================================

    @PostMapping("/{id}/solicitar-codigo")
    public ResponseEntity<Void> solicitarCodigoVerificacao(
            @PathVariable Long id,
            @RequestParam String tipo, // "SENHA" ou "NUMERO"
            @RequestParam(required = false) String novoNumero) {

        var cidadaoOpt = repository.findById(id);
        if(cidadaoOpt.isEmpty()) return ResponseEntity.notFound().build();

        Cidadao cidadao = cidadaoOpt.get();

        String codigoGerado = gerarCodigoVerificacao();
        cidadao.setCodigoVerificacao(codigoGerado);
        cidadao.setExpiracaoCodigo(java.time.LocalDateTime.now().plusMinutes(10));
        repository.save(cidadao);

        //  USA O TWILIO PARA ENVIAR SMS
        String numeroDestino = tipo.equals("NUMERO") ? novoNumero : cidadao.getTelefone();
        smsService.enviarSms(numeroDestino, codigoGerado);

        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}/alterar-senha")
    public ResponseEntity<String> alterarSenha(
            @PathVariable Long id,
            @RequestParam String codigo,
            @RequestParam String novaSenha) {

        var cidadaoOpt = repository.findById(id);
        if(cidadaoOpt.isEmpty()) return ResponseEntity.notFound().build();
        Cidadao cidadao = cidadaoOpt.get();

        if (cidadao.getCodigoVerificacao() != null && cidadao.getCodigoVerificacao().equals(codigo)) {
            if (cidadao.getExpiracaoCodigo() != null && cidadao.getExpiracaoCodigo().isBefore(java.time.LocalDateTime.now())) {
                return ResponseEntity.status(401).body("Código expirado.");
            }
            cidadao.setSenha(novaSenha);
            cidadao.setCodigoVerificacao(null);
            cidadao.setExpiracaoCodigo(null);
            repository.save(cidadao);
            return ResponseEntity.ok("Senha alterada com sucesso.");
        }
        return ResponseEntity.badRequest().body("Código inválido.");
    }

    @PutMapping("/{id}/alterar-senha-direta")
    public ResponseEntity<String> alterarSenhaDireta(
            @PathVariable Long id,
            @RequestParam String novaSenha) {

        var cidadaoOpt = repository.findById(id);
        if (cidadaoOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Cidadao cidadao = cidadaoOpt.get();
        cidadao.setSenha(novaSenha);
        repository.save(cidadao);

        return ResponseEntity.ok("Senha alterada com sucesso.");
    }

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

    @PutMapping("/{id}/alterar-numero")
    public ResponseEntity<Cidadao> alterarNumero(
            @PathVariable Long id,
            @RequestParam String codigo,
            @RequestParam String novoNumero) {

        var cidadaoOpt = repository.findById(id);
        if(cidadaoOpt.isEmpty()) return ResponseEntity.notFound().build();
        Cidadao cidadao = cidadaoOpt.get();

        if (cidadao.getCodigoVerificacao() != null && cidadao.getCodigoVerificacao().equals(codigo)) {
            if (cidadao.getExpiracaoCodigo() != null && cidadao.getExpiracaoCodigo().isBefore(java.time.LocalDateTime.now())) {
                return ResponseEntity.status(401).build();
            }
            cidadao.setTelefone(novoNumero);
            cidadao.setCodigoVerificacao(null);
            cidadao.setExpiracaoCodigo(null);
            return ResponseEntity.ok(repository.save(cidadao));
        }
        return ResponseEntity.badRequest().build();
    }

    @DeleteMapping("/{id}/excluir-conta")
    public ResponseEntity<Void> excluirConta(
            @PathVariable Long id,
            @RequestParam String codigo) {

        var cidadaoOpt = repository.findById(id);
        if(cidadaoOpt.isEmpty()) return ResponseEntity.notFound().build();
        Cidadao cidadao = cidadaoOpt.get();

        if (cidadao.getCodigoVerificacao() != null && cidadao.getCodigoVerificacao().equals(codigo)) {
            repository.delete(cidadao);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.badRequest().build();
    }

    @PutMapping("/{id}/push-token")
    public ResponseEntity<Void> salvarPushToken(
            @PathVariable Long id,
            @RequestBody String token) {

        var cidadaoOpt = repository.findById(id);
        if(cidadaoOpt.isPresent()) {
            Cidadao cidadao = cidadaoOpt.get();
            cidadao.setPushToken(token);
            repository.save(cidadao);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }

    // ==========================================
    // ROTAS PARA O "ESQUECI A SENHA" (APP DESLOGADO)
    // ==========================================

    @PostMapping("/recuperar-senha/solicitar")
    public ResponseEntity<?> solicitarCodigoRecuperacao(@RequestParam String telefone, @RequestParam String cidade) {
        var cidadaoOpt = repository.findByTelefoneAndCidade(telefone, cidade);
        if (cidadaoOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("Usuário não encontrado.");
        }

        Cidadao cidadao = cidadaoOpt.get();
        String codigo = gerarCodigoVerificacao();

        cidadao.setCodigoVerificacao(codigo);
        cidadao.setExpiracaoCodigo(java.time.LocalDateTime.now().plusMinutes(10));
        repository.save(cidadao);

        //  DISPARA O SMS VIA TWILIO
        smsService.enviarSms(cidadao.getTelefone(), codigo);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/recuperar-senha/alterar")
    public ResponseEntity<?> recuperarSenhaComCodigo(
            @RequestParam String telefone,
            @RequestParam String cidade,
            @RequestParam String codigo,
            @RequestParam String novaSenha) {

        var cidadaoOpt = repository.findByTelefoneAndCidade(telefone, cidade);
        if (cidadaoOpt.isEmpty()) return ResponseEntity.badRequest().build();

        Cidadao cidadao = cidadaoOpt.get();

        if (cidadao.getCodigoVerificacao() == null || !cidadao.getCodigoVerificacao().equals(codigo)) {
            return ResponseEntity.status(401).body("Código inválido.");
        }
        if (cidadao.getExpiracaoCodigo().isBefore(java.time.LocalDateTime.now())) {
            return ResponseEntity.status(401).body("Código expirado.");
        }

        cidadao.setSenha(novaSenha);
        cidadao.setCodigoVerificacao(null);
        cidadao.setExpiracaoCodigo(null);
        repository.save(cidadao);

        return ResponseEntity.ok().build();
    }

    // 🔴 ROTA NOVA: Valida se o código SMS está correto ANTES de deixar o usuário digitar a nova senha
    @PostMapping("/recuperar-senha/validar-codigo")
    public ResponseEntity<?> validarCodigoRecuperacao(
            @RequestParam String telefone,
            @RequestParam String cidade,
            @RequestParam String codigo) {

        var cidadaoOpt = repository.findByTelefoneAndCidade(telefone, cidade);
        if (cidadaoOpt.isEmpty()) return ResponseEntity.badRequest().build();

        Cidadao cidadao = cidadaoOpt.get();

        if (cidadao.getCodigoVerificacao() == null || !cidadao.getCodigoVerificacao().equals(codigo)) {
            return ResponseEntity.status(401).body("Código inválido.");
        }
        if (cidadao.getExpiracaoCodigo().isBefore(java.time.LocalDateTime.now())) {
            return ResponseEntity.status(401).body("Código expirado.");
        }

        return ResponseEntity.ok().build();
    }
}