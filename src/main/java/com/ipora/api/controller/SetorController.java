package com.ipora.api.controller;

import com.ipora.api.domain.Setor;
import com.ipora.api.repository.SetorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/setores")
public class SetorController {

    @Autowired
    private SetorRepository repository;

    @GetMapping
    public ResponseEntity<List<Setor>> listarSetores() {
        List<Setor> setores = repository.findAll();

        // Se a tabela estiver vazia, recria os setores padrão!
        if (setores.isEmpty()) {
            repository.save(new Setor("Infraestrutura", "construct-outline"));
            repository.save(new Setor("Iluminação Pública", "bulb-outline"));
            repository.save(new Setor("Urbanismo", "business-outline"));
            repository.save(new Setor("Limpeza Urbana", "trash-bin-outline"));
            repository.save(new Setor("Saneamento e água", "water-outline"));
            repository.save(new Setor("Saúde Pública", "medkit-outline"));

            setores = repository.findAll();
        }
        return ResponseEntity.ok(setores);
    }

    @PostMapping
    public ResponseEntity<Setor> criarSetor(@RequestBody Setor setor) {
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
}