package com.mztec.ms_matricula.producer;

import com.mztec.ms_matricula.event.MatriculaRealizadaEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class MatriculaEventProducer {

    private static final Logger log = LoggerFactory.getLogger(MatriculaEventProducer.class);
    private static final String TOPIC = "matricula-realizada";

    // KafkaTemplate<String, MatriculaRealizadaEvent>: a KEY é String (usamos o id
    // da matrícula como texto), o VALUE é o nosso record — o Spring Kafka serializa
    // esse objeto Java pra JSON automaticamente (configurado no application.yaml).
    private final KafkaTemplate<String, MatriculaRealizadaEvent> kafkaTemplate;

    public MatriculaEventProducer(KafkaTemplate<String, MatriculaRealizadaEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publicar(MatriculaRealizadaEvent evento) {
        // key = matriculaId como String: garante que mensagens da MESMA matrícula
        // sempre caiam na MESMA partição, preservando ordem (se um dia houver
        // mais de um evento por matrícula).
        //
        // send() é ASSÍNCRONO — retorna um CompletableFuture. Usamos whenComplete()
        // pra logar o resultado sem bloquear a thread principal esperando a confirmação.
        kafkaTemplate.send(TOPIC, evento.matriculaId().toString(), evento)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("FALHA ao publicar evento no Kafka para matriculaId={}: {}",
                                evento.matriculaId(), ex.getMessage(), ex);
                    } else {
                        log.info("Evento publicado com SUCESSO no tópico={}, partição={}, offset={}",
                                result.getRecordMetadata().topic(),
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    }
                });
    }
}
