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

    // Busca para o Vereador (Apenas status específicos daquela cidade)
    List<Solicitacao> findByCidadaoCidadeAndStatusInOrderByDataCriacaoDesc(String cidade, List<String> status);

    // Conta quantas solicitações uma cidade específica teve dentro de um período (ano)
    Long countByCidadaoCidadeAndDataCriacaoBetween(String cidade, java.time.LocalDateTime inicio, java.time.LocalDateTime fim);

    // CONSULTAS PARA MÉTRICAS
    long countByCidadaoCidadeAndStatus(String cidade, String status);
    long countByCidadaoCidadeAndDataCriacaoAfter(String cidade, java.time.LocalDateTime data);
    long countByCidadaoCidade(String cidade);
}