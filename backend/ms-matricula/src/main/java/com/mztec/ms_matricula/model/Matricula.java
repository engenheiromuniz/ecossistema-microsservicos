package com.mztec.ms_matricula.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "matricula")
public class Matricula {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // --- Relacionamento com ALUNO (outro microsserviço) ---
    // Aluno mora no ms-aluno, em outro banco de dados. Bancos diferentes NÃO
    // conseguem ter uma "chave estrangeira" de verdade entre si — por isso
    // guardamos só o ID (Long), e uma CÓPIA do nome (denormalização), já que
    // não dá pra simplesmente "buscar de novo" com um JOIN de SQL.
    @Column(nullable = false)
    private Long alunoId;

    @Column(nullable = false)
    private String nomeAluno;

    // --- Relacionamento com DISCIPLINA (mesmo microsserviço) ---
    // Disciplina mora no MESMO banco que Matricula. Por isso, aqui SIM podemos
    // usar um relacionamento JPA de verdade: @ManyToOne.
    //
    // Como ler "@ManyToOne" nesta classe: "MUITAS Matriculas apontam para UMA
    // Disciplina". É a leitura do ponto de vista de QUEM TEM a anotação
    // (Matricula), não do lado de quem é apontado (Disciplina).
    //
    // @JoinColumn diz qual coluna, na tabela "matricula", vai guardar o ID da
    // disciplina (a "chave estrangeira" de verdade, gerenciada pelo próprio banco).
    //
    // A vantagem prática: dado um objeto Matricula, você chama
    // matricula.getDisciplina().getNome() e o Hibernate faz o JOIN/SELECT
    // automaticamente por baixo dos panos — sem precisar guardar uma cópia
    // do nome da disciplina como fazemos com o aluno.
    @ManyToOne
    @JoinColumn(name = "disciplina_id", nullable = false)
    private Disciplina disciplina;

    private LocalDateTime dataMatricula;

    public Matricula() {
    }

    public Matricula(Long alunoId, String nomeAluno, Disciplina disciplina) {
        this.alunoId = alunoId;
        this.nomeAluno = nomeAluno;
        this.disciplina = disciplina;
        this.dataMatricula = LocalDateTime.now();
    }

    // Getters e Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getAlunoId() { return alunoId; }
    public void setAlunoId(Long alunoId) { this.alunoId = alunoId; }

    public String getNomeAluno() { return nomeAluno; }
    public void setNomeAluno(String nomeAluno) { this.nomeAluno = nomeAluno; }

    public Disciplina getDisciplina() { return disciplina; }
    public void setDisciplina(Disciplina disciplina) { this.disciplina = disciplina; }

    public LocalDateTime getDataMatricula() { return dataMatricula; }
    public void setDataMatricula(LocalDateTime dataMatricula) { this.dataMatricula = dataMatricula; }
}
