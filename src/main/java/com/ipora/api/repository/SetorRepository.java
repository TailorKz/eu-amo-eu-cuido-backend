package com.ipora.api.repository;

import com.ipora.api.domain.Setor;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SetorRepository extends JpaRepository<Setor, Long> {
    // Adiciona o OrderByIdAsc para travar a ordenação sequencial do banco
    List<Setor> findByCidadeOrderByIdAsc(String cidade);
}