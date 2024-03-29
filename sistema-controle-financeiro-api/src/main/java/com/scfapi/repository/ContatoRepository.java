package com.scfapi.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.scfapi.model.Contato;

@Repository
public interface ContatoRepository extends JpaRepository<Contato, Long> {

}
