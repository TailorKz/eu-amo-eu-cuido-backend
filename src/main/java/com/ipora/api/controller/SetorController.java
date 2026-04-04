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
    //  o app precisa enviar a cidade na requisição (ex: /api/setores?cidade=Iporã do Oeste)
    public ResponseEntity<List<Setor>> listarSetores(@RequestParam String cidade) {
        // Busca apenas os setores DAQUELA cidade
        List<Setor> setores = repository.findByCidade(cidade);

        // Se a tabela estiver vazia PARA ESTA CIDADE, cria os setores padrão
        if (setores.isEmpty()) {
            repository.save(new Setor("Infraestrutura", "construct-outline", cidade));
            repository.save(new Setor("Iluminação Pública", "bulb-outline", cidade));
            repository.save(new Setor("Urbanismo", "business-outline", cidade));
            repository.save(new Setor("Limpeza Urbana", "trash-bin-outline", cidade));
            repository.save(new Setor("Saneamento e água", "water-outline", cidade));
            repository.save(new Setor("Saúde Pública", "medkit-outline", cidade));

            setores = repository.findByCidade(cidade);
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
}