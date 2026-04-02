package com.ipora.api.repository;

import com.ipora.api.domain.ConfiguracaoPrefeitura;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConfiguracaoRepository extends JpaRepository<ConfiguracaoPrefeitura, Long> {
}