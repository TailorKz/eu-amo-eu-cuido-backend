package com.ipora.api.repository;

import com.ipora.api.domain.Cidadao;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface CidadaoRepository extends JpaRepository<Cidadao, Long> {
    // Isso aqui vai permitir buscar se um número já existe na hora do login/cadastro
    Optional<Cidadao> findByTelefone(String telefone);
}