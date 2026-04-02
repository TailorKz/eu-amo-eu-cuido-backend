package com.ipora.api.controller;

import com.ipora.api.domain.ConfiguracaoPrefeitura;
import com.ipora.api.repository.ConfiguracaoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/configuracoes")
public class ConfiguracaoController {

    @Autowired
    private ConfiguracaoRepository repository;
    @Autowired
    private com.ipora.api.repository.CidadaoRepository cidadaoRepository;

    //  ROTA DE LEITURA (Usada pelo Celular ao abrir e pelo Painel Web)
    @GetMapping
    public ResponseEntity<ConfiguracaoPrefeitura> obterConfiguracao() {
        List<ConfiguracaoPrefeitura> configs = repository.findAll();

        if (configs.isEmpty()) {
            ConfiguracaoPrefeitura configPadrao = new ConfiguracaoPrefeitura();
            configPadrao.setPopUpAtivo(false);
            configPadrao.setPopUpApenasUmaVez(true); // 🔴 NOVO (Por padrão, mostra só uma vez)
            configPadrao.setTituloPopUp("Bem-vindo!");
            configPadrao.setMensagemPopUp("Mantenha a nossa cidade limpa.");
            configPadrao.setImagemFundoLogin("");

            return ResponseEntity.ok(repository.save(configPadrao));
        }
        return ResponseEntity.ok(configs.get(0));
    }

    @PutMapping
    public ResponseEntity<ConfiguracaoPrefeitura> atualizarConfiguracao(@RequestBody ConfiguracaoPrefeitura dadosAtualizados) {
        List<ConfiguracaoPrefeitura> configs = repository.findAll();

        if (!configs.isEmpty()) {
            ConfiguracaoPrefeitura configAtual = configs.get(0);

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
    //  ROTA PARA DISPARAR NOTIFICAÇÕES PARA TODOS OS CIDADÃOS
    @PostMapping("/enviar-alerta")
    public ResponseEntity<String> enviarAlertaGeral(@RequestParam String titulo, @RequestParam String mensagem) {
        // 1. Pega em todos os cidadãos que têm um telemóvel registado no app
        List<com.ipora.api.domain.Cidadao> todosCidadaos = cidadaoRepository.findAll();

        int contagem = 0;

        for (com.ipora.api.domain.Cidadao c : todosCidadaos) {
            if (c.getPushToken() != null && c.getPushToken().startsWith("ExponentPushToken")) {
                try {
                    // 2. Monta o pacote de dados que o Expo exige
                    String jsonPayload = String.format(
                            "{\"to\": \"%s\", \"title\": \"%s\", \"body\": \"%s\", \"sound\": \"default\"}",
                            c.getPushToken(), titulo, mensagem
                    );

                    // 3. Dispara para a API oficial do Expo
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

        return ResponseEntity.ok("Alerta enviado com sucesso para " + contagem + " dispositivos!");
    }

}