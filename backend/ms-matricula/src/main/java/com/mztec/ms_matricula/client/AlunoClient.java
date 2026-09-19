package com.mztec.ms_matricula.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.mztec.ms_matricula.dto.AlunoResponseDTO;

// O método decrementarVaga() FOI REMOVIDO — o ms-aluno não expõe mais esse
// endpoint. Agora este client só serve pra uma coisa: confirmar que o aluno
// existe e pegar o nome/email dele.
@FeignClient(name = "ms-aluno")
public interface AlunoClient {

    @GetMapping("/alunos/{id}")
    AlunoResponseDTO buscarPorId(@PathVariable("id") Long id);
}
