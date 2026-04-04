package com.ipora.api.controller;

import com.ipora.api.domain.Cidadao;
import com.ipora.api.domain.ConfiguracaoPrefeitura;
import com.ipora.api.repository.ConfiguracaoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/configuracoes")
public class ConfiguracaoController {

    @Autowired
    private ConfiguracaoRepository repository;

    @Autowired
    private com.ipora.api.repository.CidadaoRepository cidadaoRepository;

    @GetMapping
    // 🔴 NOVO: Exige a cidade para retornar a imagem de fundo correta e o popup certo
    public ResponseEntity<ConfiguracaoPrefeitura> obterConfiguracao(@RequestParam String cidade) {
        Optional<ConfiguracaoPrefeitura> configOpt = repository.findByCidade(cidade);

        if (configOpt.isEmpty()) {
            ConfiguracaoPrefeitura configPadrao = new ConfiguracaoPrefeitura();
            configPadrao.setCidade(cidade); // Vincula essa configuração nova à cidade
            configPadrao.setPopUpAtivo(false);
            configPadrao.setPopUpApenasUmaVez(true);
            configPadrao.setTituloPopUp("Bem-vindo a " + cidade + "!");
            configPadrao.setMensagemPopUp("Mantenha a nossa cidade limpa.");
            configPadrao.setImagemFundoLogin("");

            return ResponseEntity.ok(repository.save(configPadrao));
        }
        return ResponseEntity.ok(configOpt.get());
    }

    @PutMapping
    // 🔴 NOVO: Exige a cidade para saber qual configuração o Prefeito está tentando atualizar
    public ResponseEntity<ConfiguracaoPrefeitura> atualizarConfiguracao(
            @RequestParam String cidade,
            @RequestBody ConfiguracaoPrefeitura dadosAtualizados) {

        Optional<ConfiguracaoPrefeitura> configOpt = repository.findByCidade(cidade);

        if (configOpt.isPresent()) {
            ConfiguracaoPrefeitura configAtual = configOpt.get();

            configAtual.setImagemFundoLogin(dadosAtualizados.getImagemFundoLogin());
            configAtual.setTituloPopUp(dadosAtualizados.getTituloPopUp());
            configAtual.setMensagemPopUp(dadosAtualizados.getMensagemPopUp());
            configAtual.setPopUpAtivo(dadosAtualizados.isPopUpAtivo());
            configAtual.setPopUpApenasUmaVez(dadosAtualizados.isPopUpApenasUmaVez());
            configAtual.setTokenTwilio(dadosAtualizados.getTokenTwilio());

            return ResponseEntity.ok(repository.save(configAtual));
        }
        return ResponseEntity.notFound().build();
    }

    //  Rota de Alerta com isolamento de cidade
    @PostMapping("/enviar-alerta")
    public ResponseEntity<String> enviarAlertaGeral(
            @RequestParam String titulo,
            @RequestParam String mensagem,
            @RequestParam String cidade) { // <-- Recebe a cidade do Painel Web

        // Busca apenas os cidadãos DAQUELA cidade
        List<Cidadao> todosCidadaos = cidadaoRepository.findByCidade(cidade);

        int contagem = 0;

        for (com.ipora.api.domain.Cidadao c : todosCidadaos) {
            if (c.getPushToken() != null && c.getPushToken().startsWith("ExponentPushToken")) {
                try {
                    String jsonPayload = String.format(
                            "{\"to\": \"%s\", \"title\": \"%s\", \"body\": \"%s\", \"sound\": \"default\"}",
                            c.getPushToken(), titulo, mensagem
                    );

                    java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                            .uri(java.net.URI.create("https://exp.host/--/api/v2/push/send"))
                            .header("Accept", "application/json")
                            .header("Content-Type", "application/json")
                            .POST(java.net.http.HttpRequest.BodyPublishers.ofString(jsonPayload))
                            .build();

                    java.net.http.HttpClient.newHttpClient().send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
                    contagem++;
                } catch (Exception e) {
                    System.out.println("Erro ao enviar push para: " + c.getNome());
                }
            }
        }

        return ResponseEntity.ok("Alerta enviado com sucesso para " + contagem + " dispositivos da cidade de " + cidade + "!");
    }
}