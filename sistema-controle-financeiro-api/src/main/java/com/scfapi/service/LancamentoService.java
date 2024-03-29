package com.scfapi.service;

import java.io.InputStream;
import java.sql.Date;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.scfapi.beans.EmailService;
import com.scfapi.config.aws.S3Service;
import com.scfapi.dto.LancamentoEstatisticaPessoaDTO;
import com.scfapi.model.Lancamento;
import com.scfapi.model.Pessoa;
import com.scfapi.model.Usuario;
import com.scfapi.repository.LancamentoRepository;
import com.scfapi.repository.PessoaRepository;
import com.scfapi.repository.UsuarioRepository;

import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;

@Service
public class LancamentoService {

	private static final String ROLEDESTINATARIOS = "ROLE_PESQUISAR_LANCAMENTO";
	
	private static final Logger logger = LoggerFactory.getLogger(Lancamento.class);
	
	@Autowired
	private S3Service s3Service;
	
	@Autowired
	private EmailService emailService;
	
	@Autowired
	private UsuarioRepository usuarioRepository;
	
	@Autowired
	private PessoaRepository pessoaRepository;

	@Autowired
	private LancamentoRepository lancamentoRepository;

	//@Scheduled(cron = "0 0 6 * * *")
	//@Scheduled(fixedDelay = 1000 * 60 * 30)
	public void servicoAvisoLancamentosVencidos() {
		
		List<Lancamento> lancamentosVencidos = lancamentoRepository
				.findByDataVencimentoLessThanEqualAndDataPagamentoIsNull(LocalDate.now());
		
		if (lancamentosVencidos.isEmpty()) {
			logger.info("Nenhum registro de lançamentos vencidos para aviso!");
			return;
		}
		logger.info("Existem {} lançamentos vencidos.", lancamentosVencidos.size());
		
		List<Usuario> destinatarios = usuarioRepository.findByPermissoesDescricao(ROLEDESTINATARIOS);
		
		if (destinatarios.isEmpty()) {
			logger.info("Existem lançamentos a serem processados, porém os "
					+ "e-mails dos destinatários não foram encontrados.");
			return;
		}
		
		emailService.avisoLancamentosVencidos(lancamentosVencidos, destinatarios);
		logger.info("E-mails de avisos enviados aos destinatários com sucesso!");
	}
	
	public Lancamento salvar(Lancamento lancamento) {
		Optional<Pessoa> pessoaSalva = pessoaRepository
				.findById(Optional.ofNullable(lancamento.getPessoa().getId()).orElse(0L));
		if (!pessoaSalva.isPresent() || !pessoaSalva.get().getAtivo()) {
			throw new PessoaInexistenteOuInativoException();
		}
		
		if (StringUtils.hasText(lancamento.getAnexo())) {
			s3Service.salvarNaBase(lancamento.getAnexo());
		}
		
		return lancamentoRepository.save(lancamento);
	}
	
	public Lancamento atualizar(Long id, Lancamento lancamento) {
		Lancamento lancamentoSalvo = existeLancamento(id);
		if (!lancamento.getPessoa().equals(lancamentoSalvo.getPessoa())) {
			existePessoa(lancamento);
		}

		if (!StringUtils.hasText(lancamento.getAnexo())
				&& StringUtils.hasText(lancamentoSalvo.getAnexo())) {
			s3Service.remover(lancamentoSalvo.getAnexo());
		} else if (StringUtils.hasText(lancamento.getAnexo())
				&& !lancamento.getAnexo().equals(lancamentoSalvo.getAnexo())) {
			s3Service.substituir(lancamentoSalvo.getAnexo(), lancamento.getAnexo());
		}
		
		BeanUtils.copyProperties(lancamento, lancamentoSalvo, "id");

		return lancamentoRepository.save(lancamentoSalvo);
	}
	
	private void existePessoa(Lancamento lancamento) {
		Optional<Pessoa> pessoa = null;
		if (lancamento.getPessoa().getId() != null) {
			pessoa = pessoaRepository.findById(lancamento.getPessoa().getId());
		}

		if (pessoa.isEmpty() || pessoa.get().isPresent()) {
			throw new IllegalArgumentException();
		}
    }

	private Lancamento existeLancamento(Long id) {
		return lancamentoRepository.findById(id).orElseThrow(() -> new IllegalArgumentException());
    }
	
	public byte[] relatorioLancamentosPessoa(LocalDate dataInicio, LocalDate dataFim) throws Exception {
		
		List<LancamentoEstatisticaPessoaDTO> dadosLancamentoPessoa = lancamentoRepository.porPessoa(dataInicio, dataFim);
		
		Map<String, Object> parametros  = new HashMap<>();
		
		parametros.put("DT_INICIO", Date.valueOf(dataInicio));
		parametros.put("DT_FIM", Date.valueOf(dataFim));
		parametros.put("REPORT_LOCALE", new Locale("pt", "BR"));
		
		InputStream inputStream = this.getClass().getResourceAsStream("/relatorios/lancamentosPorPessoa.jasper");
		
		JasperPrint jasperPrint = JasperFillManager.fillReport(inputStream, parametros, 
				new JRBeanCollectionDataSource(dadosLancamentoPessoa));
		
		return JasperExportManager.exportReportToPdf(jasperPrint);
	}
	
}
