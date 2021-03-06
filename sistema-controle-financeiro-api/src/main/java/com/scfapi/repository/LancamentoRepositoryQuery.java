package com.scfapi.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.scfapi.controller.filter.LancamentoFilter;
import com.scfapi.controller.filter.ResumoLancamento;
import com.scfapi.model.Lancamento;

public interface LancamentoRepositoryQuery {

	public Page<Lancamento> filtrar(LancamentoFilter lancamentoFilter, Pageable pageable);
	public Page<ResumoLancamento> resumir (LancamentoFilter lancamentoFilter, Pageable pageable);
}
