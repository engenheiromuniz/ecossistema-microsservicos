package com.mztec.ms_aluno.service;


import java.util.List;
import org.springframework.stereotype.Service;
import com.mztec.ms_aluno.dto.AlunoRequestDTO;
import com.mztec.ms_aluno.dto.AlunoResponseDTO;
import com.mztec.ms_aluno.model.Aluno;
import com.mztec.ms_aluno.repository.AlunoRepository;

@Service
public class AlunoService {

    private final AlunoRepository repository;

    public AlunoService(AlunoRepository repository) {
        this.repository = repository;
    }

    public AlunoResponseDTO criar(AlunoRequestDTO dto) {
        Aluno aluno = new Aluno(dto.nome(), dto.email(), dto.vagasDisponiveis());
        Aluno salvo = repository.save(aluno);
        return toResponseDTO(salvo);
    }

    public List<AlunoResponseDTO> listarTodos() {
        return repository.findAll().stream()
                .map(this::toResponseDTO)
                .toList();
    }

    public AlunoResponseDTO buscarPorId(Long id) {
        Aluno aluno = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Aluno não encontrado com id: " + id));
        return toResponseDTO(aluno);
    }

    private AlunoResponseDTO toResponseDTO(Aluno aluno) {
        return new AlunoResponseDTO(aluno.getId(), aluno.getNome(), aluno.getEmail(), aluno.getVagasDisponiveis());
    }
    
    public AlunoResponseDTO decrementarVaga(Long id) {
        Aluno aluno = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Aluno não encontrado com id: " + id));

        if (aluno.getVagasDisponiveis() == null || aluno.getVagasDisponiveis() <= 0) {
            throw new IllegalStateException("Aluno sem vagas disponíveis para matrícula");
        }

        aluno.setVagasDisponiveis(aluno.getVagasDisponiveis() - 1);
        Aluno atualizado = repository.save(aluno);
        return toResponseDTO(atualizado);
    }    
}