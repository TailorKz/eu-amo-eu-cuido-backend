package com.ipora.api.service;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ExpoNotificationService {
    private final String EXPO_PUSH_URL = "https://exp.host/--/api/v2/push/send";

    public void enviarNotificacao(List<String> pushTokens, String titulo, String mensagem) {
        // Filtra apenas os tokens válidos do Expo
        List<String> validTokens = pushTokens.stream()
                .filter(t -> t != null && t.startsWith("ExponentPushToken"))
                .collect(Collectors.toList());

        if (validTokens.isEmpty()) return;

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        List<Map<String, Object>> messages = new ArrayList<>();
        for (String token : validTokens) {
            Map<String, Object> message = new HashMap<>();
            message.put("to", token);
            message.put("sound", "default");
            message.put("title", titulo);
            message.put("body", mensagem);
            messages.add(message);
        }

        HttpEntity<List<Map<String, Object>>> request = new HttpEntity<>(messages, headers);
        try {
            restTemplate.postForObject(EXPO_PUSH_URL, request, String.class);
        } catch (Exception e) {
            System.out.println("Erro ao enviar push: " + e.getMessage());
        }
    }
}