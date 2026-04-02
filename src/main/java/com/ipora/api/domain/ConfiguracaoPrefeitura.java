package com.ipora.api.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "configuracao_prefeitura")
public class ConfiguracaoPrefeitura {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // A imagem que vai aparecer atrás do Login
    private String imagemFundoLogin;

    // Textos do Pop-up de Aviso
    private String tituloPopUp;

    @Column(columnDefinition = "TEXT") // TEXT permite textos maiores
    private String mensagemPopUp;

    // Botão de ligar/desligar o aviso
    private boolean popUpAtivo;

    private boolean popUpApenasUmaVez;

    // Chave do WhatsApp/Twilio
    private String tokenTwilio;

    // ==========================================
    // GETTERS E SETTERS (Obrigatórios no Java)
    // ==========================================

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getImagemFundoLogin() {
        return imagemFundoLogin;
    }

    public void setImagemFundoLogin(String imagemFundoLogin) {
        this.imagemFundoLogin = imagemFundoLogin;
    }

    public String getTituloPopUp() {
        return tituloPopUp;
    }

    public void setTituloPopUp(String tituloPopUp) {
        this.tituloPopUp = tituloPopUp;
    }

    public String getMensagemPopUp() {
        return mensagemPopUp;
    }

    public void setMensagemPopUp(String mensagemPopUp) {
        this.mensagemPopUp = mensagemPopUp;
    }

    public boolean isPopUpAtivo() {
        return popUpAtivo;
    }

    public void setPopUpAtivo(boolean popUpAtivo) {
        this.popUpAtivo = popUpAtivo;
    }

    public String getTokenTwilio() {
        return tokenTwilio;
    }

    public void setTokenTwilio(String tokenTwilio) {
        this.tokenTwilio = tokenTwilio;
    }

    public boolean isPopUpApenasUmaVez() {
        return popUpApenasUmaVez;
    }

    public void setPopUpApenasUmaVez(boolean popUpApenasUmaVez) {
        this.popUpApenasUmaVez = popUpApenasUmaVez;
    }
}