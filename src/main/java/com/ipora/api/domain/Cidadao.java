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

    private String cidade;

    private LocalDateTime dataCadastro = LocalDateTime.now();
}