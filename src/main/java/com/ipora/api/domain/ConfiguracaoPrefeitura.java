package com.ipora.api.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "configuracao_prefeitura")
public class ConfiguracaoPrefeitura {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String cidade;

    // --- ASSETS DINÂMICOS DA CIDADE ---
    private String logoUrl;
    private String brasaoUrl;
    private String imagemFundoLogin;

    // --- GEOFENCING (Cerca Virtual) ---
    private Double latitudeCentro;
    private Double longitudeCentro;
    private Double raioAtendimentoKm;

    // --- AVISOS POP-UP ---
    private String tituloPopUp;

    @Column(columnDefinition = "TEXT")
    private String mensagemPopUp;

    private boolean popUpAtivo;
    private boolean popUpApenasUmaVez;

    private String tokenTwilio;

    // ==========================================
    // GETTERS E SETTERS (Pode gerar via IDE ou colar estes)
    // ==========================================

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCidade() { return cidade; }
    public void setCidade(String cidade) { this.cidade = cidade; }

    public String getLogoUrl() { return logoUrl; }
    public void setLogoUrl(String logoUrl) { this.logoUrl = logoUrl; }

    public String getBrasaoUrl() { return brasaoUrl; }
    public void setBrasaoUrl(String brasaoUrl) { this.brasaoUrl = brasaoUrl; }

    public String getImagemFundoLogin() { return imagemFundoLogin; }
    public void setImagemFundoLogin(String imagemFundoLogin) { this.imagemFundoLogin = imagemFundoLogin; }

    public Double getLatitudeCentro() { return latitudeCentro; }
    public void setLatitudeCentro(Double latitudeCentro) { this.latitudeCentro = latitudeCentro; }

    public Double getLongitudeCentro() { return longitudeCentro; }
    public void setLongitudeCentro(Double longitudeCentro) { this.longitudeCentro = longitudeCentro; }

    public Double getRaioAtendimentoKm() { return raioAtendimentoKm; }
    public void setRaioAtendimentoKm(Double raioAtendimentoKm) { this.raioAtendimentoKm = raioAtendimentoKm; }

    public String getTituloPopUp() { return tituloPopUp; }
    public void setTituloPopUp(String tituloPopUp) { this.tituloPopUp = tituloPopUp; }

    public String getMensagemPopUp() { return mensagemPopUp; }
    public void setMensagemPopUp(String mensagemPopUp) { this.mensagemPopUp = mensagemPopUp; }

    public boolean isPopUpAtivo() { return popUpAtivo; }
    public void setPopUpAtivo(boolean popUpAtivo) { this.popUpAtivo = popUpAtivo; }

    public boolean isPopUpApenasUmaVez() { return popUpApenasUmaVez; }
    public void setPopUpApenasUmaVez(boolean popUpApenasUmaVez) { this.popUpApenasUmaVez = popUpApenasUmaVez; }

    public String getTokenTwilio() { return tokenTwilio; }
    public void setTokenTwilio(String tokenTwilio) { this.tokenTwilio = tokenTwilio; }
}