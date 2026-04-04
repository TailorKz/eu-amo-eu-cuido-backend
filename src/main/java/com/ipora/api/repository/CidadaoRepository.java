package com.ipora.api.repository;

import com.ipora.api.domain.Cidadao;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface CidadaoRepository extends JpaRepository<Cidadao, Long> {
    Optional<Cidadao> findByTelefoneAndCidade(String telefone, String cidade);

    // Busca todos os utilizadores de uma cidade específica
    List<Cidadao> findByCidade(String cidade);
}