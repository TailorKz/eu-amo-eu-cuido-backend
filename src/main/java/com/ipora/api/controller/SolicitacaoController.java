package com.ipora.api.controller;

import com.ipora.api.domain.Cidadao;
import com.ipora.api.domain.Solicitacao;
import com.ipora.api.repository.CidadaoRepository;
import com.ipora.api.repository.SolicitacaoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/solicitacoes")
public class SolicitacaoController {

    @Autowired
    private SolicitacaoRepository solicitacaoRepository;

    @Autowired
    private CidadaoRepository cidadaoRepository;

    // Rota para Abrir um novo Chamado
    @PostMapping("/nova/{cidadaoId}")
    public ResponseEntity<Solicitacao> criarSolicitacao(
            @PathVariable Long cidadaoId,
            @RequestBody Solicitacao novaSolicitacao) {

        // Procura quem é o cidadão que está mandando a solicitação
        var cidadaoOpt = cidadaoRepository.findById(cidadaoId);

        if (cidadaoOpt.isEmpty()) {
            return ResponseEntity.badRequest().build(); // Erro se o cidadão não existir
        }

        // Vincula o cidadão à solicitação e salva a data atual
        novaSolicitacao.setCidadao(cidadaoOpt.get());
        novaSolicitacao.setDataCriacao(LocalDateTime.now());
        novaSolicitacao.setStatus("PENDENTE");

        // Salva tudo no banco de dados!
        Solicitacao salva = solicitacaoRepository.save(novaSolicitacao);

        return ResponseEntity.ok(salva);
    }
    @GetMapping("/cidadao/{cidadaoId}")
    public ResponseEntity<List<Solicitacao>> listarPorCidadao(@PathVariable Long cidadaoId) {

        // Puxa da base de dados todos os reportos daquele ID, do mais recente para o mais antigo
        List<Solicitacao> meusReportos = solicitacaoRepository.findByCidadaoIdOrderByDataCriacaoDesc(cidadaoId);

        return ResponseEntity.ok(meusReportos);
    }
}