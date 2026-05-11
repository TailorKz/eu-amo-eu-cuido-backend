package com.ipora.api.repository;

import com.ipora.api.domain.Cidadao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface CidadaoRepository extends JpaRepository<Cidadao, Long> {
    Optional<Cidadao> findByTelefoneAndCidade(String telefone, String cidade);

    // Busca todos os utilizadores de uma cidade específica
    List<Cidadao> findByCidade(String cidade);
    // Busca a equipa de um setor específico (Gestores e Funcionários)
    List<Cidadao> findByCidadeAndSetorAtuacaoContainingAndPerfilIn(String cidade, String setorAtuacao, List<String> perfis);

    // Busca apenas gestor setor (ignora SUPER_ADMIN e PREFEITO)
    @Query("SELECT c FROM Cidadao c JOIN c.setores s WHERE s.nome = :setorNome AND c.cidade = :cidade AND c.perfil IN ('GESTOR_SETOR', 'FUNCIONARIO') AND c.pushToken IS NOT NULL")
    List<Cidadao> buscarResponsaveisDoSetorComPushToken(String setorNome, String cidade);
}