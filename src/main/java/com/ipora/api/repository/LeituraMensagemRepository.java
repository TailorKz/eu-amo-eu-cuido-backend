package com.ipora.api.repository;

import com.ipora.api.domain.LeituraMensagem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface LeituraMensagemRepository extends JpaRepository<LeituraMensagem, Long> {
    Optional<LeituraMensagem> findByUsuarioIdAndSolicitacaoId(Long usuarioId, Long solicitacaoId);
}