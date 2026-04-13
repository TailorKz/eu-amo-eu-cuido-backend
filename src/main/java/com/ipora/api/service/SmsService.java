package com.ipora.api.service;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;

@Service
public class SmsService {

    @Value("${twilio.account.sid}")
    private String accountSid;

    @Value("${twilio.auth.token}")
    private String authToken;

    @Value("${twilio.phone.number}")
    private String twilioPhoneNumber;

    @PostConstruct
    public void init() {
        Twilio.init(accountSid, authToken);
    }

    public void enviarSms(String numeroDestino, String codigo) {
        try {
            // Formata o número para o padrão internacional do Brasil
            if (!numeroDestino.startsWith("+55")) {
                numeroDestino = "+55" + numeroDestino;
            }

            String textoMensagem = "Eu Amo, Eu Cuido: O seu codigo de seguranca e *" + codigo + "*. Nao compartilhe com ninguem.";

            // Adicionar "whatsapp:" antes dos números
            Message message = Message.creator(
                    new PhoneNumber("whatsapp:" + numeroDestino),
                    new PhoneNumber("whatsapp:" + twilioPhoneNumber), // O seu número do Twilio Business
                    textoMensagem
            ).create();

            System.out.println("WhatsApp enviado! SID: " + message.getSid());

        } catch (Exception e) {
            System.err.println("Erro ao enviar WhatsApp: " + e.getMessage());
        }
    }
}