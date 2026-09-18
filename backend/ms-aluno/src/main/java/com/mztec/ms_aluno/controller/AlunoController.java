package com.mztec.ms_aluno.controller;



import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.mztec.ms_aluno.dto.AlunoRequestDTO;
import com.mztec.ms_aluno.dto.AlunoResponseDTO;
import com.mztec.ms_aluno.service.AlunoService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/alunos")   // este caminho bate com o predicate Path=/alunos/** que configuramos no Gateway
public class AlunoController {

    private final AlunoService service;

    public AlunoController(AlunoService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AlunoResponseDTO criar(@Valid @RequestBody AlunoRequestDTO dto) {
        return service.criar(dto);
    }

    @GetMapping
    public List<AlunoResponseDTO> listarTodos() {
        return service.listarTodos();
    }

    @GetMapping("/{id}")
    public AlunoResponseDTO buscarPorId(@PathVariable Long id) {
        return service.buscarPorId(id);
    }
}