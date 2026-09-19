package com.mztec.ms_aluno.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

// Agora só pede nome e email — vagasDisponiveis saiu do contrato do Aluno.
public record AlunoRequestDTO(
        @NotBlank(message = "Nome é obrigatório") String nome,
        @NotBlank @Email(message = "Email inválido") String email
) {}
