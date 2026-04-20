package com.ipora.api.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CidadaoResponseDTO {

    private Long id;
    private String nome;
    private String telefone;
    private String perfil;
    private String setorAtuacao;
    private String cidade;
    private Boolean bloqueado;
    private String token;

    // Construtor que converte a Entidade real para DTO seguro
    public CidadaoResponseDTO(Cidadao cidadao) {
        this.id = cidadao.getId();
        this.nome = cidadao.getNome();
        this.telefone = cidadao.getTelefone();
        this.perfil = cidadao.getPerfil();
        this.setorAtuacao = cidadao.getSetorAtuacao();
        this.cidade = cidadao.getCidade();
        this.bloqueado = cidadao.getBloqueado();
    }
}