package com.mztec.ms_aluno.model;

import jakarta.persistence.*;

@Entity
@Table(name = "aluno")
public class Aluno {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false, unique = true)
    private String email;

    private Integer vagasDisponiveis; // controla quantas matrículas esse aluno ainda pode fazer (só pra ter uma regra de negócio simples)

    public Aluno() {
    }

    public Aluno(String nome, String email, Integer vagasDisponiveis) {
        this.nome = nome;
        this.email = email;
        this.vagasDisponiveis = vagasDisponiveis;
    }

    // Getters e Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public Integer getVagasDisponiveis() { return vagasDisponiveis; }
    public void setVagasDisponiveis(Integer vagasDisponiveis) { this.vagasDisponiveis = vagasDisponiveis; }
}
