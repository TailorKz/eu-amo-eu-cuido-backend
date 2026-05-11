package com.ipora.api.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "leitura_mensagem")
public class LeituraMensagem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Cidadao usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "solicitacao_id")
    private Solicitacao solicitacao;

    // Guarda o ID da última mensagem que o utilizador leu neste chamado
    private Long ultimaMensagemLidaId;

    public LeituraMensagem() {}

    public LeituraMensagem(Cidadao usuario, Solicitacao solicitacao, Long ultimaMensagemLidaId) {
        this.usuario = usuario;
        this.solicitacao = solicitacao;
        this.ultimaMensagemLidaId = ultimaMensagemLidaId;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Cidadao getUsuario() { return usuario; }
    public void setUsuario(Cidadao usuario) { this.usuario = usuario; }
    public Solicitacao getSolicitacao() { return solicitacao; }
    public void setSolicitacao(Solicitacao solicitacao) { this.solicitacao = solicitacao; }
    public Long getUltimaMensagemLidaId() { return ultimaMensagemLidaId; }
    public void setUltimaMensagemLidaId(Long ultimaMensagemLidaId) { this.ultimaMensagemLidaId = ultimaMensagemLidaId; }
}