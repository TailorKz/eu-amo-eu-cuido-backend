package com.ipora.api.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class Mensagem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "TEXT")
    private String texto;

    // guarda "CIDADÃO" ou "PREFEITURA"
    private String remetente;

    private LocalDateTime dataHora;

    @ManyToOne
    @JoinColumn(name = "solicitacao_id")
    @JsonIgnore // Evita que entre em loop ao tentar ler a mensagem e a solicitação juntas
    private Solicitacao solicitacao;

    @PrePersist
    public void prePersist() {
        this.dataHora = LocalDateTime.now();
    }


    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTexto() { return texto; }
    public void setTexto(String texto) { this.texto = texto; }

    public String getRemetente() { return remetente; }
    public void setRemetente(String remetente) { this.remetente = remetente; }

    public LocalDateTime getDataHora() { return dataHora; }
    public void setDataHora(LocalDateTime dataHora) { this.dataHora = dataHora; }

    public Solicitacao getSolicitacao() { return solicitacao; }
    public void setSolicitacao(Solicitacao solicitacao) { this.solicitacao = solicitacao; }
}