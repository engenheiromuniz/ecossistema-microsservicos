package com.mztec.ms_notificacao.consumer;

import com.mztec.ms_notificacao.event.MatriculaRealizadaEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class MatriculaEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(MatriculaEventConsumer.class);

    @KafkaListener(topics = "matricula-realizada", groupId = "ms-notificacao")
    public void ouvirMatriculaRealizada(MatriculaRealizadaEvent evento) {
        log.info("=== Novo evento de matrícula recebido ===");
        log.info("Matrícula ID: {}", evento.matriculaId());
        log.info("Aluno: {} (id={})", evento.nomeAluno(), evento.alunoId());
        log.info("Disciplina: {} (id={})", evento.nomeDisciplina(), evento.disciplinaId());
        log.info("E-mail: {}", evento.emailAluno());
        log.info("Data: {}", evento.dataMatricula());

        simularEnvioDeEmail(evento);
    }

    private void simularEnvioDeEmail(MatriculaRealizadaEvent evento) {
        log.info(">>> [SIMULAÇÃO] E-mail enviado para {} ({}): você foi matriculado em '{}'",
                evento.nomeAluno(), evento.emailAluno(), evento.nomeDisciplina());
    }
}
