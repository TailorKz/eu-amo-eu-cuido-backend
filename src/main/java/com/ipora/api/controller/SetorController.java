package com.ipora.api.controller;

import com.ipora.api.domain.Setor;
import com.ipora.api.repository.SetorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

@RestController
@RequestMapping("/api/setores")
public class SetorController {

    @Autowired
    private SetorRepository repository;

    @GetMapping
    public ResponseEntity<List<Setor>> listarSetores(@RequestParam String cidade) {
        // Busca os setores filtrados por tenant e ordenados rigidamente por ID
        List<Setor> setores = repository.findByCidadeOrderByIdAsc(cidade);

        // Se a tabela estiver vazia PARA ESTA CIDADE, cria os setores na ordem de prioridade correta
        if (setores.isEmpty()) {
            repository.save(new Setor("Infraestrutura", "https://...", cidade)); // Menor ID (Primeiro)
            repository.save(new Setor("Iluminação Pública", "https://...", cidade));
            repository.save(new Setor("Urbanismo", "https://...", cidade));
            repository.save(new Setor("Limpeza Urbana", "https://...", cidade));
            repository.save(new Setor("Saneamento e Água", "https://...", cidade));
            repository.save(new Setor("Saúde Pública", "https://...", cidade)); // Maior ID (Último)

            // Recarrega já com a ordenação cravada
            setores = repository.findByCidadeOrderByIdAsc(cidade);
        }

        // --- CORREÇÃO DINÂMICA E BLINDADA PARA O ANDROID (BYPASS DA LOJA) ---
        for (Setor setor : setores) {
            if (setor.getIcone() != null && setor.getIcone().trim().startsWith("http")) {
                try {
                    // 1. Remove espaços ou quebras de linha acidentais no início ou no fim da URL
                    String urlLimpa = setor.getIcone().trim();

                    // 2. Constrói a URL codificando corretamente acentos e espaços no meio do texto
                    String urlSegura = UriComponentsBuilder
                            .fromHttpUrl(urlLimpa)
                            .build()
                            .toUriString();

                    setor.setIcone(urlSegura);
                } catch (Exception e) {
                    // Se a URL for inválida e falhar, devolve pelo menos sem os espaços ocultos nas pontas
                    setor.setIcone(setor.getIcone().trim());
                }
            }
        }

        return ResponseEntity.ok(setores);
    }

    @PostMapping
    public ResponseEntity<Setor> criarSetor(@RequestBody Setor setor) {
        // O app enviará o objeto setor já com o nome da cidade preenchido
        return ResponseEntity.ok(repository.save(setor));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletarSetor(@PathVariable Long id) {
        if(repository.existsById(id)) {
            repository.deleteById(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<Setor> atualizarSetor(@PathVariable Long id, @RequestBody Setor setorAtualizado) {
        return repository.findById(id)
                .map(setor -> {
                    setor.setNome(setorAtualizado.getNome());
                    setor.setIcone(setorAtualizado.getIcone());
                    return ResponseEntity.ok(repository.save(setor));
                })
                .orElse(ResponseEntity.notFound().build());
    }
}