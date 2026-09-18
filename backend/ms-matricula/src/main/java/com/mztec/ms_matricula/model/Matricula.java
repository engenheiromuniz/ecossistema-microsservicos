package com.mztec.ms_matricula.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "matricula")
public class Matricula {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long alunoId;

    @Column(nullable = false)
    private String nomeAluno; // guardamos uma cópia local — padrão comum em microsserviços para evitar chamadas repetidas

    private LocalDateTime dataMatricula;

    public Matricula() {
    }

    public Matricula(Long alunoId, String nomeAluno) {
        this.alunoId = alunoId;
        this.nomeAluno = nomeAluno;
        this.dataMatricula = LocalDateTime.now();
    }

    // Getters e Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getAlunoId() { return alunoId; }
    public void setAlunoId(Long alunoId) { this.alunoId = alunoId; }

    public String getNomeAluno() { return nomeAluno; }
    public void setNomeAluno(String nomeAluno) { this.nomeAluno = nomeAluno; }

    public LocalDateTime getDataMatricula() { return dataMatricula; }
    public void setDataMatricula(LocalDateTime dataMatricula) { this.dataMatricula = dataMatricula; }
}
