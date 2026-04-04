package com.ipora.api.repository;

import com.ipora.api.domain.Setor;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SetorRepository extends JpaRepository<Setor, Long> {
    //  banco a buscar setores apenas de uma cidade específica
    List<Setor> findByCidade(String cidade);
}