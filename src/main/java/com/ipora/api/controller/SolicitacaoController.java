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

    @Autowired
    private S3Client s3Client;

    @Autowired
    private com.ipora.api.service.ExpoNotificationService expoNotificationService;

    @Autowired
    private com.ipora.api.repository.MensagemRepository mensagemRepository;

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

            // LÓGICA DE GEOFENCING (Cerca Virtual)
            if (latitude != null && longitude != null) {
                // Coordenadas padrão (Iporã do Oeste)
                double centroLat = -26.9877;
                double centroLon = -53.5350;

                // Futuramente adaptar para as outras cidades da lista
                if(nomeCidade.equalsIgnoreCase("São Miguel do Oeste")){
                    centroLat = -26.7262;
                    centroLon = -53.5186;
                } else if(nomeCidade.equalsIgnoreCase("Itapiranga")){
                    centroLat = -27.1685;
                    centroLon = -53.7126;
                }

                double distancia = calcularDistancia(latitude, longitude, centroLat, centroLon);

                // Se a pessoa estiver a mais de 25km do centro da cidade, envia um erro.
                if (distancia > 25.0) {
                    return ResponseEntity.status(403).body("Erro: Adicione manualmente, sua localização atual está fora dos limites de " + nomeCidade);
                }
            }

            String nomeArquivo = UUID.randomUUID().toString() + "_" + imagem.getOriginalFilename();
            // UPLOAD DA IMAGEM PARA A AWS S3
            s3Client.putObject(PutObjectRequest.builder()
                            .bucket(bucketName)
                            .key(nomeArquivo)
                            .contentType(imagem.getContentType())
                            .build(),
                    software.amazon.awssdk.core.sync.RequestBody.fromBytes(imagem.getBytes()));

            String urlNuvem = String.format("https://%s.s3.%s.amazonaws.com/%s", bucketName, region, nomeArquivo);

            // SALVAR OS DADOS NO BANCO DE DADOS
            Solicitacao novaSolicitacao = new Solicitacao();
            novaSolicitacao.setCategoria(categoria);
            novaSolicitacao.setLocalizacao(localizacao);
            novaSolicitacao.setLatitude(latitude);
            novaSolicitacao.setLongitude(longitude);
            novaSolicitacao.setObservacao(observacao);
            novaSolicitacao.setStatus("PENDENTE");
            novaSolicitacao.setUrlImagem(urlNuvem);
            novaSolicitacao.setCidadao(cidadao);

            // GERAÇÃO INTELIGENTE DO PROTOCOLO
            String sigla = nomeCidade.length() >= 3
                    ? nomeCidade.substring(0, 3).toUpperCase().replace(" ", "")
                    : nomeCidade.toUpperCase();

            int anoAtual = java.time.LocalDate.now().getYear();
            java.time.LocalDateTime inicioAno = java.time.LocalDateTime.of(anoAtual, 1, 1, 0, 0);
            java.time.LocalDateTime fimAno = java.time.LocalDateTime.of(anoAtual, 12, 31, 23, 59, 59);

            Long contagem = solicitacaoRepository.countByCidadaoCidadeAndDataCriacaoBetween(nomeCidade, inicioAno, fimAno);
            String protocoloGerado = String.format("%s-%d-%04d", sigla, anoAtual, contagem + 1);

            novaSolicitacao.setProtocolo(protocoloGerado);

            Solicitacao solicitacaoSalva = solicitacaoRepository.save(novaSolicitacao);

            if (solicitacaoSalva.getCidadao() != null && solicitacaoSalva.getCategoria() != null) {
                List<com.ipora.api.domain.Cidadao> equipeSetor = cidadaoRepository.findByCidadeAndSetorAtuacaoAndPerfilIn(
                        solicitacaoSalva.getCidadao().getCidade(),
                        solicitacaoSalva.getCategoria(),
                        java.util.Arrays.asList("GESTOR_SETOR", "FUNCIONARIO")
                );

                List<String> tokens = equipeSetor.stream()
                        .map(com.ipora.api.domain.Cidadao::getPushToken)
                        .collect(java.util.stream.Collectors.toList());

                expoNotificationService.enviarNotificacao(
                        tokens,
                        "Nova Solicitação: " + solicitacaoSalva.getCategoria(),
                        "Um novo problema foi relatado e aguarda análise."
                );
            }

            return ResponseEntity.ok(solicitacaoSalva);

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

        Solicitacao solicitacaoSalva = solicitacaoRepository.save(s);

        if (solicitacaoSalva.getCidadao() != null && solicitacaoSalva.getCidadao().getPushToken() != null) {
            expoNotificationService.enviarNotificacao(
                    java.util.Collections.singletonList(solicitacaoSalva.getCidadao().getPushToken()),
                    "Atualização no seu chamado!",
                    "Sua solicitação foi atualizada para: " + solicitacaoSalva.getStatus().replace("_", " ")
            );
        }

        return ResponseEntity.ok(solicitacaoSalva);
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
                String nomeArquivo = UUID.randomUUID().toString() + "_resolvido_" + imagemResolvida.getOriginalFilename();

                s3Client.putObject(PutObjectRequest.builder()
                                .bucket(bucketName)
                                .key(nomeArquivo)
                                .contentType(imagemResolvida.getContentType())
                                .build(),
                        software.amazon.awssdk.core.sync.RequestBody.fromBytes(imagemResolvida.getBytes()));

                String urlNuvem = String.format("https://%s.s3.%s.amazonaws.com/%s", bucketName, region, nomeArquivo);
                s.setUrlImagemResolvida(urlNuvem);
            }

            Solicitacao solicitacaoSalva = solicitacaoRepository.save(s);

            if (solicitacaoSalva.getCidadao() != null && solicitacaoSalva.getCidadao().getPushToken() != null) {
                expoNotificationService.enviarNotificacao(
                        java.util.Collections.singletonList(solicitacaoSalva.getCidadao().getPushToken()),
                        "Atualização no seu chamado!",
                        "Sua solicitação foi atualizada para: " + solicitacaoSalva.getStatus().replace("_", " ")
                );
            }

            return ResponseEntity.ok(solicitacaoSalva);

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

    @GetMapping("/metricas")
    public ResponseEntity<java.util.Map<String, Long>> obterMetricasParaApp(
            @RequestParam String cidade,
            @RequestParam(defaultValue = "TUDO") String periodo) {

        java.time.LocalDateTime dataFiltro = null;
        java.time.LocalDateTime agora = java.time.LocalDateTime.now();

        switch (periodo) {
            case "HOJE": dataFiltro = agora.with(java.time.LocalTime.MIN); break;
            case "SEMANA": dataFiltro = agora.minusDays(7); break;
            case "MES": dataFiltro = agora.minusDays(30); break;
            case "ANO": dataFiltro = agora.minusDays(365); break;
        }

        List<Solicitacao> lista;
        if (dataFiltro != null) {
            lista = solicitacaoRepository.findByCidadaoCidadeAndDataCriacaoAfterOrderByDataCriacaoDesc(cidade, dataFiltro);
        } else {
            lista = solicitacaoRepository.findByCidadaoCidadeOrderByDataCriacaoDesc(cidade);
        }

        java.util.Map<String, Long> metricas = new java.util.HashMap<>();
        java.time.LocalDateTime inicioHoje = agora.with(java.time.LocalTime.MIN);

        metricas.put("abertasHoje", lista.stream().filter(s -> s.getDataCriacao().isAfter(inicioHoje)).count());
        metricas.put("pendentes", lista.stream().filter(s -> "PENDENTE".equals(s.getStatus())).count());
        metricas.put("emAndamento", lista.stream().filter(s -> "EM_ANDAMENTO".equals(s.getStatus())).count());
        metricas.put("resolvidas", lista.stream().filter(s -> "RESOLVIDO".equals(s.getStatus())).count());
        metricas.put("total", (long) lista.size());

        return ResponseEntity.ok(metricas);
    }

    @GetMapping("/metricas/lista")
    public ResponseEntity<List<Solicitacao>> listarMetricasDetalhe(
            @RequestParam String cidade,
            @RequestParam String periodo,
            @RequestParam String tipo
    ) {
        java.time.LocalDateTime dataFiltro = null;
        java.time.LocalDateTime agora = java.time.LocalDateTime.now();

        switch (periodo) {
            case "HOJE": dataFiltro = agora.with(java.time.LocalTime.MIN); break;
            case "SEMANA": dataFiltro = agora.minusDays(7); break;
            case "MES": dataFiltro = agora.minusDays(30); break;
            case "ANO": dataFiltro = agora.minusDays(365); break;
        }

        List<Solicitacao> lista;
        if (dataFiltro != null) {
            lista = solicitacaoRepository.findByCidadaoCidadeAndDataCriacaoAfterOrderByDataCriacaoDesc(cidade, dataFiltro);
        } else {
            lista = solicitacaoRepository.findByCidadaoCidadeOrderByDataCriacaoDesc(cidade);
        }

        java.util.stream.Stream<Solicitacao> stream = lista.stream();

        if ("ABERTAS_HOJE".equals(tipo)) {
            java.time.LocalDateTime inicioHoje = agora.with(java.time.LocalTime.MIN);
            stream = stream.filter(s -> s.getDataCriacao().isAfter(inicioHoje));
        } else if (!"TOTAL".equals(tipo)) {
            stream = stream.filter(s -> s.getStatus().equals(tipo));
        }

        return ResponseEntity.ok(stream.collect(java.util.stream.Collectors.toList()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deletarSolicitacao(@PathVariable Long id) {
        if (solicitacaoRepository.existsById(id)) {
            solicitacaoRepository.deleteById(id);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }

    // SISTEMA DE CHAT

    // Puxa o histórico de um chamado
    @GetMapping("/{id}/mensagens")
    public ResponseEntity<List<com.ipora.api.domain.Mensagem>> listarMensagens(@PathVariable Long id) {
        return ResponseEntity.ok(mensagemRepository.findBySolicitacaoIdOrderByDataHoraAsc(id));
    }

    // Envia uma nova mensagem (usado tanto pelo cidadão quanto pela prefeitura)
    @PostMapping("/{id}/mensagens")
    public ResponseEntity<com.ipora.api.domain.Mensagem> enviarMensagem(
            @PathVariable Long id,
            @RequestParam String texto,
            @RequestParam String remetente) {

        var opt = solicitacaoRepository.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();

        Solicitacao solicitacao = opt.get();

        com.ipora.api.domain.Mensagem novaMensagem = new com.ipora.api.domain.Mensagem();
        novaMensagem.setTexto(texto);
        novaMensagem.setRemetente(remetente); // "CIDADÃO" ou "PREFEITURA"
        novaMensagem.setSolicitacao(solicitacao);

        com.ipora.api.domain.Mensagem salva = mensagemRepository.save(novaMensagem);

        //SISTEMA DE NOTIFICAÇÕES BIDIRECIONAL

        // Se a PREFEITURA enviou a mensagem, notifica o cidadão
        if (remetente.equals("PREFEITURA") && solicitacao.getCidadao() != null && solicitacao.getCidadao().getPushToken() != null) {
            expoNotificationService.enviarNotificacao(
                    java.util.Collections.singletonList(solicitacao.getCidadao().getPushToken()),
                    "Nova mensagem da Prefeitura",
                    "A prefeitura respondeu no seu chamado de " + solicitacao.getCategoria()
            );
        }

        // Se o CIDADÃO enviou a mensagem, notifica os secretários do setor
        else if (remetente.equals("CIDADÃO") && solicitacao.getCidadao() != null) {

            // Vai ao banco e busca todos os GESTORES e FUNCIONÁRIOS da mesma cidade e que cuidam daquela categoria
            List<com.ipora.api.domain.Cidadao> equipeSetor = cidadaoRepository.findByCidadeAndSetorAtuacaoAndPerfilIn(
                    solicitacao.getCidadao().getCidade(),
                    solicitacao.getCategoria(),
                    java.util.Arrays.asList("GESTOR_SETOR", "FUNCIONARIO")
            );

            // Pega apenas os "Push Tokens" que não são nulos
            List<String> tokensEquipe = equipeSetor.stream()
                    .map(com.ipora.api.domain.Cidadao::getPushToken)
                    .filter(token -> token != null && !token.isEmpty())
                    .collect(java.util.stream.Collectors.toList());

            if (!tokensEquipe.isEmpty()) {
                expoNotificationService.enviarNotificacao(
                        tokensEquipe,
                        "Nova mensagem de um Cidadão",
                        "O cidadão enviou uma mensagem no chamado: " + solicitacao.getProtocolo()
                );
            }
        }

        return ResponseEntity.ok(salva);
    }
}