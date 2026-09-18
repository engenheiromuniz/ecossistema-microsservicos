package com.mztec.ms_matricula.dto;

import jakarta.validation.constraints.NotNull;

public record MatriculaRequestDTO(
        @NotNull(message = "O id do aluno é obrigatório") Long alunoId
) {}