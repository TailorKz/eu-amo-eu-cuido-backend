package com.ipora.api.repository;

import com.ipora.api.domain.ConfiguracaoPrefeitura;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ConfiguracaoRepository extends JpaRepository<ConfiguracaoPrefeitura, Long> {
    //  banco a buscar a configuração de uma cidade específica
    Optional<ConfiguracaoPrefeitura> findByCidade(String cidade);
    // Retorna apenas os nomes das cidades cadastradas
    @Query("SELECT c.cidade FROM ConfiguracaoPrefeitura c ORDER BY c.id ASC")
    List<String> listarTodasCidades();
}