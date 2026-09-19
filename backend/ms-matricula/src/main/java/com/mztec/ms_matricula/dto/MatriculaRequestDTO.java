package com.mztec.ms_matricula.dto;

import jakarta.validation.constraints.NotNull;

// Agora pede DOIS ids: quem está matriculando (alunoId) e em qual disciplina
// (disciplinaId). Exemplo de JSON:
// { "alunoId": 1, "disciplinaId": 1 }
public record MatriculaRequestDTO(
        @NotNull(message = "O id do aluno é obrigatório") Long alunoId,
        @NotNull(message = "O id da disciplina é obrigatório") Long disciplinaId
) {}
