package com.ipora.api.controller;

import com.ipora.api.domain.Cidadao;
import com.ipora.api.domain.CidadaoResponseDTO;
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

    @Autowired
    private com.ipora.api.repository.SolicitacaoRepository solicitacaoRepository;

    // MÉTODO PARA GERAR O CÓDIGO ALEATÓRIO
    private String gerarCodigoVerificacao() {
        return String.format("%04d", new java.util.Random().nextInt(10000));
    }

    @Autowired
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @Autowired
    private com.ipora.api.service.TokenService tokenService;

    private boolean isSuperAdmin(jakarta.servlet.http.HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.replace("Bearer ", "");
            return "SUPER_ADMIN".equals(tokenService.getPerfil(token));
        }
        return false;
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
    public ResponseEntity<?> cadastrarPeloApp(@RequestBody Cidadao novoCidadao) {

        var existenteOpt = repository.findByTelefoneAndCidade(novoCidadao.getTelefone(), novoCidadao.getCidade());

        if (existenteOpt.isPresent()) {
            Cidadao existente = existenteOpt.get();

            if (existente.getBloqueado() != null && existente.getBloqueado()) {
                return ResponseEntity.status(403).body("Acesso bloqueado pela administração.");
            }

            if (existente.getSenha() == null || existente.getSenha().isEmpty()) {
                // Atualiza o esqueleto com os dados reais que digitou no app
                existente.setNome(novoCidadao.getNome());
                existente.setSenha(passwordEncoder.encode(novoCidadao.getSenha()));
                repository.save(existente);

                // --- CORREÇÃO: GERA O TOKEN PARA O USUÁRIO LOGAR DIRETO ---
                String tokenGerado = tokenService.gerarToken(existente);
                CidadaoResponseDTO usuarioSeguro = new CidadaoResponseDTO(existente);
                usuarioSeguro.setToken(tokenGerado);

                return ResponseEntity.ok(usuarioSeguro);
            } else {
                return ResponseEntity.badRequest().body("Número já cadastrado.");
            }
        }

        // SE NÃO EXISTE NA LISTA VIP, É UM CIDADÃO COMUM
        novoCidadao.setPerfil("CIDADÃO");
        novoCidadao.setSenha(passwordEncoder.encode(novoCidadao.getSenha()));
        repository.save(novoCidadao);

        // --- CORREÇÃO: GERA O TOKEN PARA O USUÁRIO LOGAR DIRETO ---
        String tokenGerado = tokenService.gerarToken(novoCidadao);
        CidadaoResponseDTO usuarioSeguro = new CidadaoResponseDTO(novoCidadao);
        usuarioSeguro.setToken(tokenGerado);

        return ResponseEntity.ok(usuarioSeguro);
    }

    @PostMapping("/login")
    public ResponseEntity<CidadaoResponseDTO> login(@RequestBody Cidadao dadosLogin) {

        var cidadaoOpt = repository.findByTelefoneAndCidade(dadosLogin.getTelefone(), dadosLogin.getCidade());

        if (cidadaoOpt.isPresent()) {
            Cidadao cidadao = cidadaoOpt.get();

            if (cidadao.getBloqueado() != null && cidadao.getBloqueado()) {
                return ResponseEntity.status(403).body(null);
            }

            if (passwordEncoder.matches(dadosLogin.getSenha(), cidadao.getSenha())) {

                // Gera o Token JWT
                String tokenGerado = tokenService.gerarToken(cidadao);

                // Cria o DTO Seguro
                CidadaoResponseDTO usuarioSeguro = new CidadaoResponseDTO(cidadao);

                // Coloca o token dentro do DTO para o Front-end guardar
                usuarioSeguro.setToken(tokenGerado);

                return ResponseEntity.ok(usuarioSeguro);
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
    public ResponseEntity<?> atualizarPerfil(
            jakarta.servlet.http.HttpServletRequest request,
            @PathVariable Long id,
            @RequestBody Cidadao dadosAtualizados) {

        if (!isSuperAdmin(request)) {
            return ResponseEntity.status(403).body("Acesso negado. Apenas o SUPER ADMIN pode alterar perfis.");
        }

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
            cidadao.setSenha(passwordEncoder.encode(novaSenha));
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
        cidadao.setSenha(passwordEncoder.encode(novaSenha));
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

        if (cidadao.getBloqueado() != null && cidadao.getBloqueado()) {
            return ResponseEntity.status(403).body("Acesso bloqueado pela administração.");
        }

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

        cidadao.setSenha(passwordEncoder.encode(novaSenha));
        cidadao.setCodigoVerificacao(null);
        cidadao.setExpiracaoCodigo(null);
        repository.save(cidadao);

        return ResponseEntity.ok().build();
    }

    //  Valida se o código SMS está correto ANTES de deixar o usuário digitar a nova senha
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

    @DeleteMapping("/admin-excluir/{id}")
    public ResponseEntity<?> excluirContaPeloAdmin(
            jakarta.servlet.http.HttpServletRequest request,
            @PathVariable Long id) {

        if (!isSuperAdmin(request)) {
            return ResponseEntity.status(403).body("Acesso negado.");
        }

        var cidadaoOpt = repository.findById(id);
        if (cidadaoOpt.isPresent()) {
            Cidadao cidadao = cidadaoOpt.get();
            repository.delete(cidadao);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }

    @PostMapping("/admin-criar")
    public ResponseEntity<?> criarContaDiretaPeloAdmin(
            jakarta.servlet.http.HttpServletRequest request,
            @RequestBody Cidadao novoCidadao) {

        if (!isSuperAdmin(request)) {
            return ResponseEntity.status(403).body("Acesso negado. Apenas o SUPER ADMIN pode criar contas administrativas.");
        }

        var existente = repository.findByTelefoneAndCidade(novoCidadao.getTelefone(), novoCidadao.getCidade());
        if (existente.isPresent()) {
            return ResponseEntity.badRequest().body("Este número já está cadastrado nesta cidade.");
        }

        if (novoCidadao.getPerfil() == null || novoCidadao.getPerfil().isEmpty()) {
            novoCidadao.setPerfil("CIDADÃO");
        }
        novoCidadao.setSenha(passwordEncoder.encode(novoCidadao.getSenha()));
        repository.save(novoCidadao);

        return ResponseEntity.ok(novoCidadao);
    }

    @PostMapping("/admin-pre-aprovar")
    public ResponseEntity<?> preAprovarNumero(
            jakarta.servlet.http.HttpServletRequest request,
            @RequestBody Cidadao vip) {

        if (!isSuperAdmin(request)) {
            return ResponseEntity.status(403).body("Acesso negado.");
        }

        // Cria o esqueleto (nome e senha ficam vazios/nulos)
        Cidadao novoVip = new Cidadao();
        novoVip.setTelefone(vip.getTelefone());
        novoVip.setCidade(vip.getCidade());
        novoVip.setPerfil(vip.getPerfil());
        novoVip.setSetorAtuacao(vip.getSetorAtuacao());

        repository.save(novoVip);
        return ResponseEntity.ok().build();
    }

    //  ROTA PARA BANIR/DESBANIR USUÁRIO
    @PutMapping("/{id}/bloquear")
    public ResponseEntity<?> alternarBloqueioUsuario(
            jakarta.servlet.http.HttpServletRequest request,
            @PathVariable Long id,
            @RequestParam boolean bloquear) {

        if (!isSuperAdmin(request)) {
            return ResponseEntity.status(403).body("Acesso negado.");
        }

        var cidadaoOpt = repository.findById(id);
        if (cidadaoOpt.isPresent()) {
            Cidadao cidadao = cidadaoOpt.get();
            cidadao.setBloqueado(bloquear);
            if (bloquear) cidadao.setPushToken(null);
            repository.save(cidadao);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }

    // Promover um cidadão já existente a um cargo (Usado no "Adicionar Membro")
    @PutMapping("/promover-por-telefone")
    public ResponseEntity<?> promoverPorTelefone(
            jakarta.servlet.http.HttpServletRequest request,
            @RequestParam String telefone,
            @RequestParam String cidade,
            @RequestParam String perfil,
            @RequestParam String setorAtuacao) {

        if (!isSuperAdmin(request)) {
            return ResponseEntity.status(403).body("Acesso negado. Apenas o SUPER ADMIN pode alterar cargos.");
        }

        var cidadaoOpt = repository.findByTelefoneAndCidade(telefone, cidade);

        if (cidadaoOpt.isPresent()) {
            Cidadao cidadao = cidadaoOpt.get();

            // Verifica se está banido
            if (cidadao.getBloqueado() != null && cidadao.getBloqueado()) {
                return ResponseEntity.status(403).body("Este utilizador está banido.");
            }

            cidadao.setPerfil(perfil);
            cidadao.setSetorAtuacao(setorAtuacao);
            repository.save(cidadao);
            return ResponseEntity.ok(cidadao);
        }

        // Retorna erro se o cidadão não existir na base de dados
        return ResponseEntity.status(404).body("Utilizador não encontrado.");
    }
}