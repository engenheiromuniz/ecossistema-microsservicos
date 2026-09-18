package com.mztec.ms_matricula.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.mztec.ms_matricula.dto.AlunoResponseDTO;

// "ms-aluno" é o NOME LÓGICO registrado no Eureka (spring.application.name daquele serviço).
// O Feign resolve esse nome em um endereço real via LoadBalancer + Eureka — igual o "lb://" do Gateway.
@FeignClient(name = "ms-aluno")
public interface AlunoClient {

    @GetMapping("/alunos/{id}")
    AlunoResponseDTO buscarPorId(@PathVariable("id") Long id);          // chamada síncrona #1

    @PatchMapping("/alunos/{id}/decrementar-vaga")
    AlunoResponseDTO decrementarVaga(@PathVariable("id") Long id);      // chamada síncrona #2
}
