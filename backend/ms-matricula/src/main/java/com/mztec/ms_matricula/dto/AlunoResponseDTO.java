package com.mztec.ms_matricula.dto;

//Este DTO espelha o formato de resposta que ms-aluno expõe.
//Em microsserviços, cada serviço tem seus próprios DTOs "de visão" —
//não compartilhamos classes entre projetos, evitando acoplamento forte.
public record AlunoResponseDTO(
     Long id,
     String nome,
     String email,
     Integer vagasDisponiveis
) {}