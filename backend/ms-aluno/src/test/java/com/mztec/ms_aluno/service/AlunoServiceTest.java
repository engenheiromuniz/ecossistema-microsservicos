package com.mztec.ms_aluno.service;

import com.mztec.ms_aluno.dto.AlunoRequestDTO;
import com.mztec.ms_aluno.dto.AlunoResponseDTO;
import com.mztec.ms_aluno.model.Aluno;
import com.mztec.ms_aluno.repository.AlunoRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

// @ExtendWith(MockitoExtension.class) "liga" o Mockito nesta classe de teste.
// Sem essa anotação, as anotações @Mock e @InjectMocks logo abaixo não
// funcionariam — o JUnit não saberia que precisa processá-las.
@ExtendWith(MockitoExtension.class)
class AlunoServiceTest {

    // @Mock cria um "dublê" (um objeto falso) do AlunoRepository.
    // Ele TEM os mesmos métodos da interface real (save, findById, findAll...),
    // mas nenhum deles realmente conversa com um banco de dados — por padrão,
    // cada método retorna "vazio"/null até a gente ensinar (com when(...)) o
    // que ele deve devolver em cada cenário.
    //
    // Por que usar um dublê em vez do banco H2 de verdade? Porque queremos
    // testar SÓ a lógica do AlunoService, isoladamente — sem depender de um
    // banco estar configurado, sem risco de um teste "sujar" dados que outro
    // teste também usa, e rodando em milissegundos em vez de segundos.
    @Mock
    private AlunoRepository repository;

    // @InjectMocks cria uma instância REAL de AlunoService, mas injeta os
    // @Mock declarados acima (no caso, o "repository" falso) no lugar das
    // dependências reais. É como testar o motor de um carro na bancada,
    // sem precisar colocar o carro inteiro na estrada.
    @InjectMocks
    private AlunoService service;

    @Test
    void criar_deveSalvarERetornarAlunoComId() {
        // CENÁRIO: preparamos os dados de entrada e ensinamos o mock a se
        // comportar como se o banco tivesse realmente salvo o aluno.
        AlunoRequestDTO dto = new AlunoRequestDTO("Ana Maria Braga", "ana.maria@email.com");

        // "quando alguém chamar repository.save(qualquerAluno), devolva um
        // Aluno com id=1 preenchido" — simulando o que o banco faria de verdade
        // (gerar um ID automático ao salvar).
        when(repository.save(any(Aluno.class))).thenAnswer(invocation -> {
            Aluno alunoRecebido = invocation.getArgument(0);
            alunoRecebido.setId(1L);
            return alunoRecebido;
        });

        // AÇÃO: chamamos o método que estamos testando de verdade.
        AlunoResponseDTO resultado = service.criar(dto);

        // VERIFICAÇÃO: conferimos se o resultado é exatamente o esperado.
        assertThat(resultado.id()).isEqualTo(1L);
        assertThat(resultado.nome()).isEqualTo("Ana Maria Braga");
        assertThat(resultado.email()).isEqualTo("ana.maria@email.com");

        // Bônus: confirmamos que o método save() do repository foi
        // REALMENTE chamado uma vez — se o AlunoService "esquecesse" de
        // salvar, esse teste pegaria o erro.
        verify(repository, times(1)).save(any(Aluno.class));
    }

    @Test
    void buscarPorId_quandoAlunoExiste_deveRetornarAluno() {
        Aluno alunoExistente = new Aluno("João Silva", "joao@email.com");
        alunoExistente.setId(5L);

        // Optional.of(...) simula "o banco encontrou o registro"
        when(repository.findById(5L)).thenReturn(Optional.of(alunoExistente));

        AlunoResponseDTO resultado = service.buscarPorId(5L);

        assertThat(resultado.id()).isEqualTo(5L);
        assertThat(resultado.nome()).isEqualTo("João Silva");
    }

    @Test
    void buscarPorId_quandoAlunoNaoExiste_deveLancarExcecao() {
        // Optional.empty() simula "o banco procurou e não achou nada"
        when(repository.findById(999L)).thenReturn(Optional.empty());

        // assertThrows verifica que o código DENTRO do lambda realmente
        // lança a exceção esperada — se o AlunoService não lançasse nada
        // (ou lançasse outro tipo de exceção), este teste falharia.
        assertThrows(RuntimeException.class, () -> service.buscarPorId(999L));
    }

    @Test
    void listarTodos_deveRetornarTodosOsAlunosCadastrados() {
        Aluno aluno1 = new Aluno("Ana", "ana@email.com");
        aluno1.setId(1L);
        Aluno aluno2 = new Aluno("Bruno", "bruno@email.com");
        aluno2.setId(2L);

        when(repository.findAll()).thenReturn(List.of(aluno1, aluno2));

        List<AlunoResponseDTO> resultado = service.listarTodos();

        assertThat(resultado).hasSize(2);
        assertThat(resultado.get(0).nome()).isEqualTo("Ana");
        assertThat(resultado.get(1).nome()).isEqualTo("Bruno");
    }
}
