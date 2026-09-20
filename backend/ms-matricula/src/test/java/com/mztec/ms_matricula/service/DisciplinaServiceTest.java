package com.mztec.ms_matricula.service;

import com.mztec.ms_matricula.dto.DisciplinaRequestDTO;
import com.mztec.ms_matricula.dto.DisciplinaResponseDTO;
import com.mztec.ms_matricula.model.Disciplina;
import com.mztec.ms_matricula.repository.DisciplinaRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DisciplinaServiceTest {

    @Mock
    private DisciplinaRepository repository;

    @InjectMocks
    private DisciplinaService service;

    @Test
    void criar_deveSalvarERetornarDisciplinaComId() {
        DisciplinaRequestDTO dto = new DisciplinaRequestDTO("Cálculo I", 5);

        when(repository.save(any(Disciplina.class))).thenAnswer(invocation -> {
            Disciplina d = invocation.getArgument(0);
            d.setId(1L);
            return d;
        });

        DisciplinaResponseDTO resultado = service.criar(dto);

        assertThat(resultado.id()).isEqualTo(1L);
        assertThat(resultado.nome()).isEqualTo("Cálculo I");
        assertThat(resultado.vagasDisponiveis()).isEqualTo(5);
    }

    @Test
    void decrementarVaga_quandoTemVagaDisponivel_deveDecrementarEmUm() {
        // CENÁRIO: uma disciplina com 5 vagas.
        Disciplina disciplina = new Disciplina("Cálculo I", 5);
        disciplina.setId(1L);

        when(repository.findById(1L)).thenReturn(Optional.of(disciplina));
        // Aqui devolvemos o MESMO objeto que recebemos — simulando um save()
        // que apenas persiste o estado atualizado, sem criar um objeto novo.
        when(repository.save(any(Disciplina.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // AÇÃO
        Disciplina resultado = service.decrementarVaga(1L);

        // VERIFICAÇÃO: a vaga caiu de 5 para 4.
        assertThat(resultado.getVagasDisponiveis()).isEqualTo(4);
    }

    @Test
    void decrementarVaga_quandoNaoHaVagasDisponiveis_deveLancarExcecao() {
        // CENÁRIO: uma disciplina já com 0 vagas — o caso que motivou toda
        // a regra de negócio existir.
        Disciplina disciplinaLotada = new Disciplina("Cálculo I", 0);
        disciplinaLotada.setId(1L);

        when(repository.findById(1L)).thenReturn(Optional.of(disciplinaLotada));

        // Verificamos que a exceção certa é lançada QUANDO NÃO HÁ VAGA —
        // esse é o teste mais importante deste projeto, porque é a regra
        // de negócio central que motivou toda a refatoração de modelagem.
        IllegalStateException excecao = assertThrows(
                IllegalStateException.class,
                () -> service.decrementarVaga(1L)
        );

        assertThat(excecao.getMessage()).contains("sem vagas disponíveis");

        // Bônus importante: garantimos que o save() NUNCA foi chamado —
        // ou seja, nada foi persistido no banco quando a operação falha.
        // "verify(repository, never())" confirma a AUSÊNCIA de uma chamada,
        // o oposto de "verify(repository, times(1))" que usamos no outro teste.
        org.mockito.Mockito.verify(repository, org.mockito.Mockito.never()).save(any());
    }

    @Test
    void buscarEntidadePorId_quandoNaoExiste_deveLancarExcecao() {
        when(repository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> service.buscarEntidadePorId(999L));
    }
}
