package com.ipora.api.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "setor")
public class Setor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nome;

    // Nome do ícone da biblioteca Ionicons do celular
    private String icone;

    // Construtores
    public Setor() {}

    public Setor(String nome, String icone) {
        this.nome = nome;
        this.icone = icone;
    }

    // Getters e Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getIcone() { return icone; }
    public void setIcone(String icone) { this.icone = icone; }
}