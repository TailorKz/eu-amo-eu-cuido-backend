package com.ipora.api.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "setor")
public class Setor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nome;
    private String icone;

    // Coluna para identificar de qual cidade é este setor
    private String cidade;

    public Setor() {}

    // Atualizado o construtor para receber a cidade
    public Setor(String nome, String icone, String cidade) {
        this.nome = nome;
        this.icone = icone;
        this.cidade = cidade;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getIcone() { return icone; }
    public void setIcone(String icone) { this.icone = icone; }

    //  Getters e Setters da cidade
    public String getCidade() { return cidade; }
    public void setCidade(String cidade) { this.cidade = cidade; }
}