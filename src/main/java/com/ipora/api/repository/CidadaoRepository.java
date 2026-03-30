package com.ipora.api.repository;

import com.ipora.api.domain.Cidadao;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface CidadaoRepository extends JpaRepository<Cidadao, Long> {
    // BUSCA PELO TELEFONE E PELA CIDADE
    Optional<Cidadao> findByTelefoneAndCidade(String telefone, String cidade);
}