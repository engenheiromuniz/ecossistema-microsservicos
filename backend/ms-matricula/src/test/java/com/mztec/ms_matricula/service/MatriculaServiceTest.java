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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

// Este é o teste mais rico do projeto, porque MatriculaService é um
// "ORQUESTRADOR": ele não faz o trabalho pesado sozinho, ele COORDENA quatro
// outras peças (AlunoClient, DisciplinaService, MatriculaRepository,
// MatriculaEventProducer). Testar um orquestrador é, basicamente, testar se
// ele chama as peças certas, na ORDEM certa, e reage direito quando uma
// delas falha no meio do caminho.
@ExtendWith(MockitoExtension.class)
class MatriculaServiceTest {

    @Mock
    private MatriculaRepository repository;

    @Mock
    private AlunoClient alunoClient;

    @Mock
    private DisciplinaService disciplinaService;

    @Mock
    private MatriculaEventProducer eventProducer;

    @InjectMocks
    private MatriculaService service;

    @Test
    void matricular_fluxoFeliz_deveOrquestrarTudoNaOrdemCorreta() {
        // CENÁRIO
        MatriculaRequestDTO dto = new MatriculaRequestDTO(1L, 10L);

        AlunoResponseDTO aluno = new AlunoResponseDTO(1L, "Ana Maria Braga", "ana.maria@email.com");

        // Este objeto representa a Disciplina JÁ COM A VAGA DECREMENTADA —
        // porque quem decrementa é o DisciplinaService (que aqui é um mock),
        // então simulamos o resultado que ele DEVOLVERIA depois de decrementar.
        Disciplina disciplinaAposDecremento = new Disciplina("Cálculo I", 4);
        disciplinaAposDecremento.setId(10L);

        when(alunoClient.buscarPorId(1L)).thenReturn(aluno);
        when(disciplinaService.decrementarVaga(10L)).thenReturn(disciplinaAposDecremento);
        when(repository.save(any(Matricula.class))).thenAnswer(invocation -> {
            Matricula m = invocation.getArgument(0);
            m.setId(100L);
            return m;
        });

        // AÇÃO
        MatriculaResponseDTO resultado = service.matricular(dto);

        // VERIFICAÇÃO 1: o resultado devolvido está correto.
        assertThat(resultado.id()).isEqualTo(100L);
        assertThat(resultado.alunoId()).isEqualTo(1L);
        assertThat(resultado.nomeAluno()).isEqualTo("Ana Maria Braga");
        assertThat(resultado.disciplinaId()).isEqualTo(10L);
        assertThat(resultado.nomeDisciplina()).isEqualTo("Cálculo I");

        // VERIFICAÇÃO 2: a ORDEM das chamadas importa! InOrder garante que
        // "buscar aluno" aconteceu ANTES de "decrementar vaga", que aconteceu
        // ANTES de "salvar matrícula", que aconteceu ANTES de "publicar evento".
        // Se alguém no futuro reordenar essas linhas dentro de
        // MatriculaService.matricular() por engano, este teste vai falhar.
        InOrder ordem = inOrder(alunoClient, disciplinaService, repository, eventProducer);
        ordem.verify(alunoClient).buscarPorId(1L);
        ordem.verify(disciplinaService).decrementarVaga(10L);
        ordem.verify(repository).save(any(Matricula.class));
        ordem.verify(eventProducer).publicar(any(MatriculaRealizadaEvent.class));

        // VERIFICAÇÃO 3: o CONTEÚDO do evento publicado está correto.
        // ArgumentCaptor "captura" o objeto que foi passado pro método mockado,
        // pra gente poder inspecionar seus campos depois — útil quando o
        // retorno do método é void (como publicar()) e não dá pra simplesmente
        // conferir o "resultado" de um jeito direto.
        ArgumentCaptor<MatriculaRealizadaEvent> captor = ArgumentCaptor.forClass(MatriculaRealizadaEvent.class);
        verify(eventProducer).publicar(captor.capture());

        MatriculaRealizadaEvent eventoPublicado = captor.getValue();
        assertThat(eventoPublicado.matriculaId()).isEqualTo(100L);
        assertThat(eventoPublicado.alunoId()).isEqualTo(1L);
        assertThat(eventoPublicado.nomeDisciplina()).isEqualTo("Cálculo I");
    }

    @Test
    void matricular_quandoAlunoNaoExiste_naoDeveTocarEmNadaDepois() {
        // CENÁRIO: o Feign lança exceção porque o ms-aluno respondeu 404.
        // (Aqui simulamos só o efeito — não fazemos uma chamada de rede de verdade.)
        when(alunoClient.buscarPorId(999L))
                .thenThrow(new RuntimeException("Aluno não encontrado com id: 999"));

        MatriculaRequestDTO dto = new MatriculaRequestDTO(999L, 10L);

        // AÇÃO + VERIFICAÇÃO: a exceção deve "estourar" pra fora do método.
        assertThrows(RuntimeException.class, () -> service.matricular(dto));

        // VERIFICAÇÃO IMPORTANTE: como o aluno nem existe, NADA do que vem
        // depois no método deveria ter sido executado — nem decremento de
        // vaga, nem salvamento, nem publicação de evento. Isso prova que o
        // método falha "cedo" (fail-fast), sem deixar efeitos colaterais
        // pela metade.
        verify(disciplinaService, never()).decrementarVaga(anyLong());
        verify(repository, never()).save(any());
        verify(eventProducer, never()).publicar(any());
    }

    @Test
    void matricular_quandoDisciplinaSemVaga_naoDeveSalvarNemPublicarEvento() {
        // CENÁRIO: o aluno existe normalmente...
        AlunoResponseDTO aluno = new AlunoResponseDTO(1L, "Ana Maria Braga", "ana.maria@email.com");
        when(alunoClient.buscarPorId(1L)).thenReturn(aluno);

        // ...mas a disciplina está lotada.
        when(disciplinaService.decrementarVaga(10L))
                .thenThrow(new IllegalStateException("Disciplina sem vagas disponíveis: Cálculo I"));

        MatriculaRequestDTO dto = new MatriculaRequestDTO(1L, 10L);

        assertThrows(IllegalStateException.class, () -> service.matricular(dto));

        // O aluno JÁ TINHA sido buscado com sucesso antes da falha —
        // diferente do teste anterior, aqui o primeiro passo funcionou.
        verify(alunoClient, times(1)).buscarPorId(1L);

        // Mas nada depois da falha da disciplina deveria ter acontecido.
        verify(repository, never()).save(any());
        verify(eventProducer, never()).publicar(any());
    }
}
