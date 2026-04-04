package com.ipora.api.repository;

import com.ipora.api.domain.Solicitacao;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SolicitacaoRepository extends JpaRepository<Solicitacao, Long> {
    List<Solicitacao> findByCidadaoIdOrderByDataCriacaoDesc(Long cidadaoId);

    // Busca todas as solicitações de uma cidade (através da cidade do cidadão)
    List<Solicitacao> findByCidadaoCidadeOrderByDataCriacaoDesc(String cidade);

    //  Busca as solicitações de um setor específico, MAS restrito à cidade!
    List<Solicitacao> findByCategoriaAndCidadaoCidadeOrderByDataCriacaoDesc(String categoria, String cidade);
}