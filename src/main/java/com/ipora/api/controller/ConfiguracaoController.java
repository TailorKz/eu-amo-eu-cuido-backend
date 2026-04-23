package com.ipora.api.controller;

import com.ipora.api.domain.Cidadao;
import com.ipora.api.domain.ConfiguracaoPrefeitura;
import com.ipora.api.repository.ConfiguracaoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/configuracoes")
public class ConfiguracaoController {

    @Autowired
    private ConfiguracaoRepository repository;

    @Autowired
    private com.ipora.api.repository.CidadaoRepository cidadaoRepository;

    @Autowired
    private com.ipora.api.service.ExpoNotificationService expoNotificationService;

    @GetMapping
    public ResponseEntity<ConfiguracaoPrefeitura> obterConfiguracao(@RequestParam String cidade) {
        Optional<ConfiguracaoPrefeitura> configOpt = repository.findByCidade(cidade);

        if (configOpt.isEmpty()) {
            ConfiguracaoPrefeitura configPadrao = new ConfiguracaoPrefeitura();
            configPadrao.setCidade(cidade);
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

            return ResponseEntity.ok(repository.save(configAtual));
        }
        return ResponseEntity.notFound().build();
    }

    // ROTA DE ALERTA
    @PostMapping("/enviar-alerta")
    public ResponseEntity<String> enviarAlertaGeral(
            @RequestParam String titulo,
            @RequestParam String mensagem,
            @RequestParam String cidade) {

        List<Cidadao> todosCidadaos = cidadaoRepository.findByCidade(cidade);

        // Coleta todos os tokens válidos e apaga os repetidos (.distinct)
        List<String> tokensUnicos = todosCidadaos.stream()
                .map(Cidadao::getPushToken)
                .filter(t -> t != null && t.startsWith("ExponentPushToken"))
                .distinct() //
                .collect(Collectors.toList());

        // Envia de uma só vez
        if (!tokensUnicos.isEmpty()) {
            expoNotificationService.enviarNotificacao(tokensUnicos, titulo, mensagem);
        }

        return ResponseEntity.ok("Alerta enviado com sucesso para " + tokensUnicos.size() + " dispositivos da cidade de " + cidade + "!");
    }
}