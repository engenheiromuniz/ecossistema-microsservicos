package com.mztec.ms_aluno.service;

import java.util.List;
import org.springframework.stereotype.Service;
import com.mztec.ms_aluno.dto.AlunoRequestDTO;
import com.mztec.ms_aluno.dto.AlunoResponseDTO;
import com.mztec.ms_aluno.model.Aluno;
import com.mztec.ms_aluno.repository.AlunoRepository;

// O método decrementarVaga() FOI REMOVIDO daqui — essa regra de negócio agora
// pertence ao DisciplinaService, dentro do ms-matricula, porque "vagas" é uma
// propriedade da Disciplina, não do Aluno.
@Service
public class AlunoService {

    private final AlunoRepository repository;

    public AlunoService(AlunoRepository repository) {
        this.repository = repository;
    }

    public AlunoResponseDTO criar(AlunoRequestDTO dto) {
        Aluno aluno = new Aluno(dto.nome(), dto.email());
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
        return new AlunoResponseDTO(aluno.getId(), aluno.getNome(), aluno.getEmail());
    }
}
