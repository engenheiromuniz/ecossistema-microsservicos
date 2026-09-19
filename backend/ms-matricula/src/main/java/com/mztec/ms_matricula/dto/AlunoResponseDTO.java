package com.mztec.ms_matricula.dto;

// Espelha o formato de resposta ATUAL do ms-aluno — agora sem vagasDisponiveis,
// já que essa informação não existe mais do lado do Aluno.
public record AlunoResponseDTO(
        Long id,
        String nome,
        String email
) {}
