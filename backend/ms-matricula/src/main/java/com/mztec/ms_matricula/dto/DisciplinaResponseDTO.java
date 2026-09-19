package com.mztec.ms_matricula.dto;

public record DisciplinaResponseDTO(
        Long id,
        String nome,
        Integer vagasDisponiveis
) {}
