package com.mztec.ms_matricula.dto;

import java.time.LocalDateTime;

public record MatriculaResponseDTO(
        Long id,
        Long alunoId,
        String nomeAluno,
        Long disciplinaId,
        String nomeDisciplina,
        LocalDateTime dataMatricula
) {}
