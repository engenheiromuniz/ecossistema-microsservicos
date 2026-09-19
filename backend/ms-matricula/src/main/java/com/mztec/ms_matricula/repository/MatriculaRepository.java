package com.mztec.ms_matricula.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mztec.ms_matricula.model.Matricula;

public interface MatriculaRepository extends JpaRepository<Matricula, Long> {

}
