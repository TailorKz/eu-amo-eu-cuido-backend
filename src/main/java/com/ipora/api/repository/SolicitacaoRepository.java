package com.ipora.api.repository;

import com.ipora.api.domain.Solicitacao;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SolicitacaoRepository extends JpaRepository<Solicitacao, Long> {
    // Busca todas as solicitações de um cidadão específico
    List<Solicitacao> findByCidadaoIdOrderByDataCriacaoDesc(Long cidadaoId);
}