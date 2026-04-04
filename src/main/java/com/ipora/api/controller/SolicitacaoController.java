package com.ipora.api.controller;

import com.ipora.api.domain.Solicitacao;
import com.ipora.api.repository.CidadaoRepository;
import com.ipora.api.repository.SolicitacaoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
// REMOVEMOS o import conflitante do RequestBody da Amazon aqui!
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
            // 1. Inicia o cliente S3
            S3Client s3 = S3Client.builder().build();

            // 2. Gera um nome único profissional para a foto
            String nomeArquivo = UUID.randomUUID().toString() + "_" + imagem.getOriginalFilename();

            // 3. Faz o Upload direto para a Amazon (Usando o nome completo para evitar erro)
            s3.putObject(PutObjectRequest.builder()
                            .bucket(bucketName)
                            .key(nomeArquivo)
                            .contentType(imagem.getContentType())
                            .build(),
                    software.amazon.awssdk.core.sync.RequestBody.fromBytes(imagem.getBytes()));

            // 4. Cria a URL pública real da internet
            String urlNuvem = String.format("https://%s.s3.%s.amazonaws.com/%s", bucketName, region, nomeArquivo);

            // 5. Salva no Banco de Dados do Railway
            Solicitacao novaSolicitacao = new Solicitacao();
            novaSolicitacao.setCategoria(categoria);
            novaSolicitacao.setLocalizacao(localizacao);
            novaSolicitacao.setObservacao(observacao);
            novaSolicitacao.setStatus("PENDENTE");
            novaSolicitacao.setUrlImagem(urlNuvem);

            var cidadaoOpt = cidadaoRepository.findById(cidadaoId);
            if(cidadaoOpt.isPresent()){
                novaSolicitacao.setCidadao(cidadaoOpt.get());
            }

            return ResponseEntity.ok(solicitacaoRepository.save(novaSolicitacao));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
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

    // Rota exclusiva para o App Celular atualizar a solicitação enviando uma foto
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

            // Se o funcionário enviou uma foto, faz o upload para a Amazon S3
            if (imagemResolvida != null && !imagemResolvida.isEmpty()) {
                S3Client s3 = S3Client.builder().build();
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

    // Rota para o Painel Web (Super Admin) - Traz tudo de uma cidade
    @GetMapping("/cidade/{cidade}")
    public ResponseEntity<List<Solicitacao>> listarPorCidade(@PathVariable String cidade) {
        return ResponseEntity.ok(solicitacaoRepository.findByCidadaoCidadeOrderByDataCriacaoDesc(cidade));
    }

    // Rota para o Painel Web (Funcionário) - Traz do setor dele, na cidade dele
    @GetMapping("/setor/{setor}")
    public ResponseEntity<List<Solicitacao>> listarPorSetorECidade(
            @PathVariable String setor,
            @RequestParam String cidade) {
        return ResponseEntity.ok(solicitacaoRepository.findByCategoriaAndCidadaoCidadeOrderByDataCriacaoDesc(setor, cidade));
    }
}