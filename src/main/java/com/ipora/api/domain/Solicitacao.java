package com.ipora.api.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "tb_solicitacao")
public class Solicitacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String categoria; // Infraestrutura, Iluminação, etc.

    private String localizacao; // A rua que o GPS pegou

    @Column(columnDefinition = "TEXT")
    private String observacao;

    private String urlImagem; // O link da foto armazenada na nuvem

    private String status = "PENDENTE"; // PENDENTE, EM_ANDAMENTO, RESOLVIDO

    private LocalDateTime dataCriacao = LocalDateTime.now();

    // Uma Solicitação pertence a um Cidadão
    @ManyToOne
    @JoinColumn(name = "cidadao_id", nullable = false)
    private Cidadao cidadao;

    @Column(columnDefinition = "TEXT")
    private String resposta;

}