package com.ipora.api.controller;

import com.ipora.api.domain.Cidadao;
import com.ipora.api.domain.Solicitacao;
import com.ipora.api.repository.CidadaoRepository;
import com.ipora.api.repository.SolicitacaoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

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
    // 🔴 NOVA Rota para Criar Solicitação (Agora recebendo arquivo pesado!)
    @PostMapping(value = "/nova/{cidadaoId}", consumes = {"multipart/form-data"})
    public ResponseEntity<Solicitacao> criarSolicitacao(
            @PathVariable Long cidadaoId,
            @RequestParam("categoria") String categoria,
            @RequestParam("localizacao") String localizacao,
            @RequestParam(value = "observacao", required = false) String observacao,
            @RequestParam("imagem") MultipartFile imagem) { // 🔴 AQUI ELE RECEBE A FOTO REAL

        try {
            // 1. Define onde a imagem vai ser salva no seu PC (Disco C)
            String pastaDestino = "C:/ipora_imagens/";
            Path caminhoPasta = Paths.get(pastaDestino);

            // Se a pasta não existir no seu PC, o Java cria ela na hora!
            if (!Files.exists(caminhoPasta)) {
                Files.createDirectories(caminhoPasta);
            }

            // 2. Gera um nome único para a foto (para não substituir fotos com o mesmo nome)
            String nomeArquivo = UUID.randomUUID().toString() + "_" + imagem.getOriginalFilename();
            Path caminhoArquivo = caminhoPasta.resolve(nomeArquivo);

            // 3. SALVA O ARQUIVO FISICAMENTE NO COMPUTADOR
            imagem.transferTo(caminhoArquivo.toFile());

            // 4. Salva os dados no Banco de Dados (PostgreSQL)
            Solicitacao novaSolicitacao = new Solicitacao();
            novaSolicitacao.setCategoria(categoria);
            novaSolicitacao.setLocalizacao(localizacao);
            novaSolicitacao.setObservacao(observacao);
            novaSolicitacao.setStatus("PENDENTE");
            // Salva apenas o caminho de texto no banco!
            novaSolicitacao.setUrlImagem("file:///" + pastaDestino + nomeArquivo);

            // Associa o cidadão ao chamado (Assumindo que você tem o cidadaoRepository)
            var cidadaoOpt = cidadaoRepository.findById(cidadaoId);
            if(cidadaoOpt.isPresent()){
                novaSolicitacao.setCidadao(cidadaoOpt.get());
            }

            Solicitacao salva = solicitacaoRepository.save(novaSolicitacao);
            return ResponseEntity.ok(salva);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/cidadao/{cidadaoId}")
    public ResponseEntity<List<Solicitacao>> listarPorCidadao(@PathVariable Long cidadaoId) {

        // Puxa da base de dados todos os reportos daquele ID, do mais recente para o mais antigo
        List<Solicitacao> meusReportos = solicitacaoRepository.findByCidadaoIdOrderByDataCriacaoDesc(cidadaoId);

        return ResponseEntity.ok(meusReportos);
    }

    //  Rota para Atualizar (Editar ou Mudar Status/Setor/Resposta)
    @PutMapping("/{id}")
    public ResponseEntity<Solicitacao> atualizarSolicitacao(
            @PathVariable Long id,
            @RequestBody Solicitacao dadosAtualizados) {

        var solicitacaoOpt = solicitacaoRepository.findById(id);
        if (solicitacaoOpt.isEmpty()) return ResponseEntity.notFound().build();

        Solicitacao solicitacaoExistente = solicitacaoOpt.get();

        // Cidadão edita:
        if (dadosAtualizados.getLocalizacao() != null) solicitacaoExistente.setLocalizacao(dadosAtualizados.getLocalizacao());
        if (dadosAtualizados.getObservacao() != null) solicitacaoExistente.setObservacao(dadosAtualizados.getObservacao());

        // Prefeitura edita:
        if (dadosAtualizados.getStatus() != null) solicitacaoExistente.setStatus(dadosAtualizados.getStatus());
        if (dadosAtualizados.getCategoria() != null) solicitacaoExistente.setCategoria(dadosAtualizados.getCategoria()); // 🔴 Permite transferir
        if (dadosAtualizados.getResposta() != null) solicitacaoExistente.setResposta(dadosAtualizados.getResposta()); // 🔴 Guarda a resposta

        return ResponseEntity.ok(solicitacaoRepository.save(solicitacaoExistente));
    }

    //  Rota para Excluir
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluirSolicitacao(@PathVariable Long id) {
        if (!solicitacaoRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }

        solicitacaoRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    //  Rota para listar os chamados de um setor específico da prefeitura
    @GetMapping("/setor/{categoria}")
    public ResponseEntity<List<Solicitacao>> listarPorSetor(@PathVariable String categoria) {
        List<Solicitacao> chamadosDoSetor = solicitacaoRepository.findByCategoriaOrderByDataCriacaoDesc(categoria);
        return ResponseEntity.ok(chamadosDoSetor);
    }

    //  Rota para o Painel Web (Super Admin) - Traz TODAS as solicitações da cidade
    @GetMapping("/todas")
    public ResponseEntity<List<Solicitacao>> listarTodasAsSolicitacoes() {
        return ResponseEntity.ok(solicitacaoRepository.findAll());
    }

}