package com.mztec.ms_matricula.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record DisciplinaRequestDTO(
        @NotBlank(message = "Nome da disciplina é obrigatório") String nome,
        @NotNull(message = "Informe as vagas disponíveis") Integer vagasDisponiveis
) {}
