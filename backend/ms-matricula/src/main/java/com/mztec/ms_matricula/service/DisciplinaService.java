package com.mztec.ms_matricula.service;

import java.util.List;
import org.springframework.stereotype.Service;
import com.mztec.ms_matricula.dto.DisciplinaRequestDTO;
import com.mztec.ms_matricula.dto.DisciplinaResponseDTO;
import com.mztec.ms_matricula.model.Disciplina;
import com.mztec.ms_matricula.repository.DisciplinaRepository;

@Service
public class DisciplinaService {

    private final DisciplinaRepository repository;

    public DisciplinaService(DisciplinaRepository repository) {
        this.repository = repository;
    }

    public DisciplinaResponseDTO criar(DisciplinaRequestDTO dto) {
        Disciplina disciplina = new Disciplina(dto.nome(), dto.vagasDisponiveis());
        Disciplina salva = repository.save(disciplina);
        return toResponseDTO(salva);
    }

    public List<DisciplinaResponseDTO> listarTodas() {
        return repository.findAll().stream()
                .map(this::toResponseDTO)
                .toList();
    }

    public DisciplinaResponseDTO buscarPorId(Long id) {
        return toResponseDTO(buscarEntidadePorId(id));
    }

    // Retorna a ENTIDADE (não o DTO) — usado internamente pelo MatriculaService,
    // que precisa do objeto Disciplina de verdade para montar o relacionamento
    // JPA dentro de Matricula (veja MatriculaService.matricular()).
    public Disciplina buscarEntidadePorId(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Disciplina não encontrada com id: " + id));
    }

    // Repare a diferença com o Feign do AlunoClient: aqui é uma chamada de método
    // Java NORMAL, dentro do mesmo processo — sem rede, sem serialização JSON,
    // sem risco de timeout de outro serviço. É por isso que colocamos Disciplina
    // no mesmo microsserviço que Matricula: essa operação precisa ser rápida e
    // "tudo ou nada" junto com o salvamento da matrícula.
    public Disciplina decrementarVaga(Long id) {
        Disciplina disciplina = buscarEntidadePorId(id);

        if (disciplina.getVagasDisponiveis() == null || disciplina.getVagasDisponiveis() <= 0) {
            throw new IllegalStateException("Disciplina sem vagas disponíveis: " + disciplina.getNome());
        }

        disciplina.setVagasDisponiveis(disciplina.getVagasDisponiveis() - 1);
        return repository.save(disciplina);
    }

    private DisciplinaResponseDTO toResponseDTO(Disciplina d) {
        return new DisciplinaResponseDTO(d.getId(), d.getNome(), d.getVagasDisponiveis());
    }
}
