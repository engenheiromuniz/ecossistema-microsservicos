package com.mztec.ms_matricula.event;

import java.time.LocalDateTime;

// Contrato da mensagem publicada no Kafka. Agora inclui dados da disciplina
// também — o ms-notificacao precisa da MESMA lista de campos, na sua própria
// cópia deste record (ver ms-notificacao/event/MatriculaRealizadaEvent.java).
public record MatriculaRealizadaEvent(
        Long matriculaId,
        Long alunoId,
        String nomeAluno,
        String emailAluno,
        Long disciplinaId,
        String nomeDisciplina,
        LocalDateTime dataMatricula
) {}
