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
// NOVA REGRA: O mesmo telefone não pode se repetir NA MESMA CIDADE
@Table(name = "tb_cidadao", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"telefone", "cidade"})
})
public class Cidadao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nome;

    @Column(nullable = false)
    private String telefone;

    private String senha;


    private java.time.LocalDateTime expiracaoCodigo;

//  NOVAS COLUNAS PARA O SISTEMA WEB

    // Define o poder do usuário: "CIDADAO", "FUNCIONARIO", "GESTOR_SETOR", "SUPER_ADMIN"
    private String perfil = "CIDADAO";

    // Se for um Gestor ou Funcionário, diz de qual setor ele é (ex: "Infraestrutura").
    // Para Cidadão e Super Admin, isso ficará vazio (null).
    private String setorAtuacao;

    private String cidade;

    private LocalDateTime dataCadastro = LocalDateTime.now();

    // Campo para guardar o código gerado temporariamente
    private String codigoVerificacao;

    // Token para enviar notificações Push para o telemóvel
    private String pushToken;

    private Boolean bloqueado = false;

    public Boolean getBloqueado() { return bloqueado; }
    public void setBloqueado(Boolean bloqueado) { this.bloqueado = bloqueado; }
}

