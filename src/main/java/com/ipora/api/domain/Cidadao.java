package com.ipora.api.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data // O Lombok gera todos os Getters e Setters automaticamente
@NoArgsConstructor
@AllArgsConstructor
@Entity // Avisa ao Spring que isso vai virar uma tabela no banco
@Table(name = "tb_cidadao")
public class Cidadao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nome;

    @Column(unique = true, nullable = false)
    private String telefone; // Será usado para o login

    private String senha;

    private String cidade;

    private LocalDateTime dataCadastro = LocalDateTime.now();
}