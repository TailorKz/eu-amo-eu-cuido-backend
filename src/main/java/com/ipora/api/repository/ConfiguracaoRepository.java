package com.ipora.api.repository;

import com.ipora.api.domain.ConfiguracaoPrefeitura;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ConfiguracaoRepository extends JpaRepository<ConfiguracaoPrefeitura, Long> {
    //  banco a buscar a configuração de uma cidade específica
    Optional<ConfiguracaoPrefeitura> findByCidade(String cidade);
}