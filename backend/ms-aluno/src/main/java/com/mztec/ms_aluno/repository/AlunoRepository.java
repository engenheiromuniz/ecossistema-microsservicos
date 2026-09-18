package com.mztec.ms_aluno.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mztec.ms_aluno.model.Aluno;

public interface AlunoRepository extends JpaRepository<Aluno, Long>{

}
