package com.mztec.ms_aluno.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mztec.ms_aluno.model.Aluno;

// Ao estender JpaRepository<Aluno, Long>, ganhamos de graça métodos como
// save(), findById(), findAll(), deleteById(), etc — sem escrever nenhum SQL.
public interface AlunoRepository extends JpaRepository<Aluno, Long> {

}
