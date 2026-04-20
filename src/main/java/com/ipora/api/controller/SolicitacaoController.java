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

    private double calcularDistancia(double lat1, double lon1, double lat2, double lon2) {
        final int RAIO_TERRA = 6371; // Raio da Terra em Km

        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);

        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return RAIO_TERRA * c; // Retorna a distância em Quilômetros
    }

    @PostMapping(value = "/nova/{cidadaoId}", consumes = {"multipart/form-data"})
    public ResponseEntity<?> criarSolicitacao(
            @PathVariable Long cidadaoId,
            @RequestParam("categoria") String categoria,
            @RequestParam("localizacao") String localizacao,
            @RequestParam(value = "latitude", required = false) Double latitude,
            @RequestParam(value = "longitude", required = false) Double longitude,
            @RequestParam(value = "observacao", required = false) String observacao,
            @RequestParam("imagem") MultipartFile imagem) {

        try {
            var cidadaoOpt = cidadaoRepository.findById(cidadaoId);
            if(cidadaoOpt.isEmpty()) {
                return ResponseEntity.badRequest().body("Cidadão não encontrado.");
            }

            com.ipora.api.domain.Cidadao cidadao = cidadaoOpt.get();
            String nomeCidade = cidadao.getCidade();

            // 1. LÓGICA DE GEOFENCING (Cerca Virtual)
            if (latitude != null && longitude != null) {
                // Coordenadas padrão (Iporã do Oeste)
                double centroLat = -26.9877;
                double centroLon = -53.5350;

                // Adapte para as outras cidades da sua lista
                if(nomeCidade.equalsIgnoreCase("São Miguel do Oeste")){
                    centroLat = -26.7262;
                    centroLon = -53.5186;
                } else if(nomeCidade.equalsIgnoreCase("Itapiranga")){
                    centroLat = -27.1685;
                    centroLon = -53.7126;
                }

                double distancia = calcularDistancia(latitude, longitude, centroLat, centroLon);

                // Se a pessoa estiver a mais de 25km do centro da cidade, barra!
                if (distancia > 30.0) {
                    return ResponseEntity.status(403).body("Erro: Adicione manualmente, sua localização atual está fora dos limites de " + nomeCidade);
                }
            }

            // 2. UPLOAD DA IMAGEM PARA A AWS S3
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

            // 3. SALVAR OS DADOS NO BANCO DE DADOS
            Solicitacao novaSolicitacao = new Solicitacao();
            novaSolicitacao.setCategoria(categoria);
            novaSolicitacao.setLocalizacao(localizacao);
            novaSolicitacao.setLatitude(latitude);   // Salva a Latitude
            novaSolicitacao.setLongitude(longitude); // Salva a Longitude
            novaSolicitacao.setObservacao(observacao);
            novaSolicitacao.setStatus("PENDENTE");
            novaSolicitacao.setUrlImagem(urlNuvem);
            novaSolicitacao.setCidadao(cidadao);

            // 4. GERAÇÃO INTELIGENTE DO PROTOCOLO
            String sigla = nomeCidade.length() >= 3
                    ? nomeCidade.substring(0, 3).toUpperCase().replace(" ", "")
                    : nomeCidade.toUpperCase();

            int anoAtual = java.time.LocalDate.now().getYear();
            java.time.LocalDateTime inicioAno = java.time.LocalDateTime.of(anoAtual, 1, 1, 0, 0);
            java.time.LocalDateTime fimAno = java.time.LocalDateTime.of(anoAtual, 12, 31, 23, 59, 59);

            Long contagem = solicitacaoRepository.countByCidadaoCidadeAndDataCriacaoBetween(nomeCidade, inicioAno, fimAno);
            String protocoloGerado = String.format("%s-%d-%04d", sigla, anoAtual, contagem + 1);

            novaSolicitacao.setProtocolo(protocoloGerado);

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