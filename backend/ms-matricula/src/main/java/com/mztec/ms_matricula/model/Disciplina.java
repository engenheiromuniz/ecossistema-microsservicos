package com.mztec.ms_matricula.model;

import jakarta.persistence.*;

// Disciplina é o "recurso compartilhado": várias matrículas podem apontar pra
// UMA mesma disciplina, todas disputando o mesmo pool de vagas.
// Ela mora no MESMO banco de dados que Matricula (dentro do ms-matricula) —
// por isso conseguimos criar um relacionamento JPA de verdade entre elas
// (veja a classe Matricula.java, no campo "disciplina").
@Entity
@Table(name = "disciplina")
public class Disciplina {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false)
    private Integer vagasDisponiveis;

    public Disciplina() {
    }

    public Disciplina(String nome, Integer vagasDisponiveis) {
        this.nome = nome;
        this.vagasDisponiveis = vagasDisponiveis;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public Integer getVagasDisponiveis() { return vagasDisponiveis; }
    public void setVagasDisponiveis(Integer vagasDisponiveis) { this.vagasDisponiveis = vagasDisponiveis; }
}
