package com.ipora.api.repository;

import com.ipora.api.domain.Mensagem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MensagemRepository extends JpaRepository<Mensagem, Long> {
    // Traz o histórico do chat ordenado da mensagem mais velha para a mais nova
    List<Mensagem> findBySolicitacaoIdOrderByDataHoraAsc(Long solicitacaoId);
}