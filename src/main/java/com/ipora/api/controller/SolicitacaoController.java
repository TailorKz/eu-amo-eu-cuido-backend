package com.ipora.api.controller;

import com.ipora.api.domain.Solicitacao;
import com.ipora.api.repository.CidadaoRepository;
import com.ipora.api.repository.SolicitacaoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/solicitacoes")
public class SolicitacaoController {

    @Autowired
    private SolicitacaoRepository solicitacaoRepository;

    @Autowired
    private CidadaoRepository cidadaoRepository;

    @Value("${aws.s3.bucket}")
    private String bucketName;

    @Value("${aws.region}")
    private String region;

    @PostMapping(value = "/nova/{cidadaoId}", consumes = {"multipart/form-data"})
    public ResponseEntity<Solicitacao> criarSolicitacao(
            @PathVariable Long cidadaoId,
            @RequestParam("categoria") String categoria,
            @RequestParam("localizacao") String localizacao,
            @RequestParam(value = "observacao", required = false) String observacao,
            @RequestParam("imagem") MultipartFile imagem) {

        try {
            // 🔴 CORREÇÃO AQUI: Dizemos ao Java exatamente qual é a Região da Amazon!
            S3Client s3 = S3Client.builder()
                    .region(software.amazon.awssdk.regions.Region.of(region))
                    .build();

            String nomeArquivo = UUID.randomUUID().toString() + "_" + imagem.getOriginalFilename();

            s3.putObject(PutObjectRequest.builder()
                            .bucket(bucketName)
                            .key(nomeArquivo)
                            .contentType(imagem.getContentType())
                            .build(),
                    software.amazon.awssdk.core.sync.RequestBody.fromBytes(imagem.getBytes()));

            String urlNuvem = String.format("https://%s.s3.%s.amazonaws.com/%s", bucketName, region, nomeArquivo);

            Solicitacao novaSolicitacao = new Solicitacao();
            novaSolicitacao.setCategoria(categoria);
            novaSolicitacao.setLocalizacao(localizacao);
            novaSolicitacao.setObservacao(observacao);
            novaSolicitacao.setStatus("PENDENTE");
            novaSolicitacao.setUrlImagem(urlNuvem);

            var cidadaoOpt = cidadaoRepository.findById(cidadaoId);
            if(cidadaoOpt.isPresent()){
                com.ipora.api.domain.Cidadao cidadao = cidadaoOpt.get();
                novaSolicitacao.setCidadao(cidadao);

                String nomeCidade = cidadao.getCidade();
                String sigla = nomeCidade.length() >= 3
                        ? nomeCidade.substring(0, 3).toUpperCase().replace(" ", "")
                        : nomeCidade.toUpperCase();

                int anoAtual = java.time.LocalDate.now().getYear();
                java.time.LocalDateTime inicioAno = java.time.LocalDateTime.of(anoAtual, 1, 1, 0, 0);
                java.time.LocalDateTime fimAno = java.time.LocalDateTime.of(anoAtual, 12, 31, 23, 59, 59);

                Long contagem = solicitacaoRepository.countByCidadaoCidadeAndDataCriacaoBetween(nomeCidade, inicioAno, fimAno);
                String protocoloGerado = String.format("%s-%d-%04d", sigla, anoAtual, contagem + 1);

                novaSolicitacao.setProtocolo(protocoloGerado);
            }

            return ResponseEntity.ok(solicitacaoRepository.save(novaSolicitacao));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build(); // <--- É este erro que estava a rebentar!
        }
    }

    @GetMapping("/cidadao/{cidadaoId}")
    public ResponseEntity<List<Solicitacao>> listarPorCidadao(@PathVariable Long cidadaoId) {
        return ResponseEntity.ok(solicitacaoRepository.findByCidadaoIdOrderByDataCriacaoDesc(cidadaoId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Solicitacao> atualizarSolicitacao(@PathVariable Long id, @RequestBody Solicitacao dados) {
        var opt = solicitacaoRepository.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        Solicitacao s = opt.get();
        if (dados.getStatus() != null) s.setStatus(dados.getStatus());
        if (dados.getResposta() != null) s.setResposta(dados.getResposta());
        return ResponseEntity.ok(solicitacaoRepository.save(s));
    }

    @PutMapping(value = "/{id}/atualizar-com-foto", consumes = {"multipart/form-data"})
    public ResponseEntity<Solicitacao> atualizarComFoto(
            @PathVariable Long id,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "categoria", required = false) String categoria,
            @RequestParam(value = "resposta", required = false) String resposta,
            @RequestParam(value = "imagemResolvida", required = false) MultipartFile imagemResolvida) {

        try {
            var opt = solicitacaoRepository.findById(id);
            if (opt.isEmpty()) return ResponseEntity.notFound().build();
            Solicitacao s = opt.get();

            if (status != null) s.setStatus(status);
            if (categoria != null) s.setCategoria(categoria);
            if (resposta != null) s.setResposta(resposta);

            if (imagemResolvida != null && !imagemResolvida.isEmpty()) {
                // 🔴 CORREÇÃO AQUI TAMBÉM (Força a Região)
                S3Client s3 = S3Client.builder()
                        .region(software.amazon.awssdk.regions.Region.of(region))
                        .build();

                String nomeArquivo = UUID.randomUUID().toString() + "_resolvido_" + imagemResolvida.getOriginalFilename();

                s3.putObject(PutObjectRequest.builder()
                                .bucket(bucketName)
                                .key(nomeArquivo)
                                .contentType(imagemResolvida.getContentType())
                                .build(),
                        software.amazon.awssdk.core.sync.RequestBody.fromBytes(imagemResolvida.getBytes()));

                String urlNuvem = String.format("https://%s.s3.%s.amazonaws.com/%s", bucketName, region, nomeArquivo);
                s.setUrlImagemResolvida(urlNuvem);
            }

            return ResponseEntity.ok(solicitacaoRepository.save(s));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/cidade/{cidade}")
    public ResponseEntity<List<Solicitacao>> listarPorCidade(@PathVariable String cidade) {
        return ResponseEntity.ok(solicitacaoRepository.findByCidadaoCidadeOrderByDataCriacaoDesc(cidade));
    }

    @GetMapping("/setor/{setor}")
    public ResponseEntity<List<Solicitacao>> listarPorSetorECidade(
            @PathVariable String setor,
            @RequestParam String cidade) {
        return ResponseEntity.ok(solicitacaoRepository.findByCategoriaAndCidadaoCidadeOrderByDataCriacaoDesc(setor, cidade));
    }

    @GetMapping("/vereador")
    public ResponseEntity<List<Solicitacao>> listarParaVereador(@RequestParam String cidade) {
        List<String> statusPermitidos = java.util.Arrays.asList("EM_ANDAMENTO", "RESOLVIDO");
        return ResponseEntity.ok(solicitacaoRepository.findByCidadaoCidadeAndStatusInOrderByDataCriacaoDesc(cidade, statusPermitidos));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deletarSolicitacao(@PathVariable Long id) {
        if (solicitacaoRepository.existsById(id)) {
            solicitacaoRepository.deleteById(id);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }
}