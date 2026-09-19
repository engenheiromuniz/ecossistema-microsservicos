package com.mztec.ms_matricula.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.mztec.ms_matricula.dto.DisciplinaRequestDTO;
import com.mztec.ms_matricula.dto.DisciplinaResponseDTO;
import com.mztec.ms_matricula.service.DisciplinaService;

import jakarta.validation.Valid;

// Endpoint só pra você conseguir CRIAR disciplinas de teste via Postman —
// a lógica de decrementar vaga não é exposta aqui, só é usada internamente
// pelo fluxo de matrícula (ver MatriculaService).
@RestController
@RequestMapping("/disciplinas")
public class DisciplinaController {

    private final DisciplinaService service;

    public DisciplinaController(DisciplinaService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DisciplinaResponseDTO criar(@Valid @RequestBody DisciplinaRequestDTO dto) {
        return service.criar(dto);
    }

    @GetMapping
    public List<DisciplinaResponseDTO> listarTodas() {
        return service.listarTodas();
    }

    @GetMapping("/{id}")
    public DisciplinaResponseDTO buscarPorId(@PathVariable Long id) {
        return service.buscarPorId(id);
    }
}
