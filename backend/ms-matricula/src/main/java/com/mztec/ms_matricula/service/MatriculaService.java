package com.mztec.ms_matricula.service;

import com.mztec.ms_matricula.client.AlunoClient;
import com.mztec.ms_matricula.dto.AlunoResponseDTO;
import com.mztec.ms_matricula.dto.MatriculaRequestDTO;
import com.mztec.ms_matricula.dto.MatriculaResponseDTO;
import com.mztec.ms_matricula.event.MatriculaRealizadaEvent;
import com.mztec.ms_matricula.model.Disciplina;
import com.mztec.ms_matricula.model.Matricula;
import com.mztec.ms_matricula.producer.MatriculaEventProducer;
import com.mztec.ms_matricula.repository.MatriculaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class MatriculaService {

    private final MatriculaRepository repository;
    private final AlunoClient alunoClient;
    private final DisciplinaService disciplinaService;
    private final MatriculaEventProducer eventProducer;

    public MatriculaService(MatriculaRepository repository, AlunoClient alunoClient,
                             DisciplinaService disciplinaService, MatriculaEventProducer eventProducer) {
        this.repository = repository;
        this.alunoClient = alunoClient;
        this.disciplinaService = disciplinaService;
        this.eventProducer = eventProducer;
    }

    // @Transactional garante que "decrementar vaga da disciplina" e "salvar a
    // matrícula" aconteçam como UMA operação atômica no banco local: se algo
    // der errado no meio do caminho, o Spring desfaz (rollback) as duas coisas
    // juntas — você nunca fica com uma vaga decrementada sem a matrícula salva.
    //
    // IMPORTANTE: essa garantia de atomicidade vale só para as operações no
    // banco DESTE serviço. A chamada ao ms-aluno (via Feign, HTTP) NÃO entra
    // nessa transação — ela já aconteceu antes, é uma chamada de rede
    // independente. Isso é uma limitação real e conhecida de sistemas
    // distribuídos: não existe "transação única" abrangendo dois
    // microsserviços diferentes sem ferramentas bem mais complexas (sagas,
    // por exemplo). Para este projeto de aprendizado, aceitamos essa limitação.
    @Transactional
    public MatriculaResponseDTO matricular(MatriculaRequestDTO dto) {
        // 1) Chamada SÍNCRONA via REDE (Feign/HTTP) para outro microsserviço:
        // só confirma que o aluno existe e pega nome/email dele.
        AlunoResponseDTO aluno = alunoClient.buscarPorId(dto.alunoId());

        // 2) Chamada de MÉTODO JAVA NORMAL (mesmo processo, mesmo banco):
        // busca a disciplina, valida vaga e já decrementa.
        Disciplina disciplina = disciplinaService.decrementarVaga(dto.disciplinaId());

        // 3) Monta e salva a Matricula, com o relacionamento JPA de verdade
        // apontando pro objeto Disciplina que acabamos de buscar/atualizar.
        Matricula matricula = new Matricula(aluno.id(), aluno.nome(), disciplina);
        Matricula salva = repository.save(matricula);

        // 4) Dispara o evento assíncrono no Kafka — não bloqueia o retorno HTTP.
        MatriculaRealizadaEvent evento = new MatriculaRealizadaEvent(
                salva.getId(),
                aluno.id(),
                aluno.nome(),
                aluno.email(),
                disciplina.getId(),
                disciplina.getNome(),
                salva.getDataMatricula()
        );
        eventProducer.publicar(evento);

        return toResponseDTO(salva);
    }

    public List<MatriculaResponseDTO> listarTodas() {
        return repository.findAll().stream()
                .map(this::toResponseDTO)
                .toList();
    }

    private MatriculaResponseDTO toResponseDTO(Matricula m) {
        // m.getDisciplina().getNome() -> aqui o Hibernate faz o JOIN por baixo
        // dos panos pra buscar o nome da disciplina relacionada. Não precisamos
        // guardar uma cópia desse nome em Matricula, como fazemos com o aluno.
        return new MatriculaResponseDTO(
                m.getId(),
                m.getAlunoId(),
                m.getNomeAluno(),
                m.getDisciplina().getId(),
                m.getDisciplina().getNome(),
                m.getDataMatricula()
        );
    }
}
