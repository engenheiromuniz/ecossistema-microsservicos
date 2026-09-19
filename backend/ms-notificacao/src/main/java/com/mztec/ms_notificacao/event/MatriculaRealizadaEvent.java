package com.mztec.ms_notificacao.event;

import java.time.LocalDateTime;

// CÓPIA LOCAL do contrato definido no ms-matricula. Os campos precisam ser
// IDÊNTICOS (mesmos nomes, mesma ordem de tipos) ao record do lado produtor —
// foi atualizado aqui para incluir disciplinaId e nomeDisciplina, exatamente
// como mudou do lado do ms-matricula.
public record MatriculaRealizadaEvent(
        Long matriculaId,
        Long alunoId,
        String nomeAluno,
        String emailAluno,
        Long disciplinaId,
        String nomeDisciplina,
        LocalDateTime dataMatricula
) {}
