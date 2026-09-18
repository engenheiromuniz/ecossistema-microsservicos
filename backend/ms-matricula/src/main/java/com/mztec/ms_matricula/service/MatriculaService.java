package com.mztec.ms_matricula.service;


import java.util.List;

import org.springframework.stereotype.Service;

import com.mztec.ms_matricula.client.AlunoClient;
import com.mztec.ms_matricula.dto.AlunoResponseDTO;
import com.mztec.ms_matricula.dto.MatriculaRequestDTO;
import com.mztec.ms_matricula.dto.MatriculaResponseDTO;
import com.mztec.ms_matricula.model.Matricula;
import com.mztec.ms_matricula.repository.MatriculaRepository;

@Service
public class MatriculaService {

    private final MatriculaRepository repository;
    private final AlunoClient alunoClient;

    public MatriculaService(MatriculaRepository repository, AlunoClient alunoClient) {
        this.repository = repository;
        this.alunoClient = alunoClient;
    }

    public MatriculaResponseDTO matricular(MatriculaRequestDTO dto) {
        // Chamada síncrona #1: valida que o aluno existe e recupera os dados dele
        AlunoResponseDTO aluno = alunoClient.buscarPorId(dto.alunoId());

        // Chamada síncrona #2: reserva a vaga (efeito colateral em ms-aluno)
        alunoClient.decrementarVaga(dto.alunoId());

        // Persiste a matrícula localmente
        Matricula matricula = new Matricula(aluno.id(), aluno.nome());
        Matricula salva = repository.save(matricula);

        // (Na Fase 6 vamos inserir aqui o disparo do evento Kafka — ainda não agora)

        return toResponseDTO(salva);
    }

    public List<MatriculaResponseDTO> listarTodas() {
        return repository.findAll().stream()
                .map(this::toResponseDTO)
                .toList();
    }

    private MatriculaResponseDTO toResponseDTO(Matricula m) {
        return new MatriculaResponseDTO(m.getId(), m.getAlunoId(), m.getNomeAluno(), m.getDataMatricula());
    }
}
