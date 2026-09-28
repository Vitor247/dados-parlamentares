package com.example.parlamento.config;

import com.example.parlamento.exception.AcessoNegadoException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Exige a chave de administração no header {@value #HEADER}. As exceções lançadas
 * aqui passam pelo {@code @RestControllerAdvice} e saem no formato StandardError.
 */
public class AdminApiKeyInterceptor implements HandlerInterceptor {

	public static final String HEADER = "X-Admin-Key";

	private final byte[] chaveEsperada;

	public AdminApiKeyInterceptor(SegurancaProperties props) {
		this.chaveEsperada = props.adminHabilitado() ? props.adminApiKey().getBytes(StandardCharsets.UTF_8) : null;
	}

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
		if (chaveEsperada == null) {
			throw AcessoNegadoException.administracaoDesabilitada();
		}
		String recebida = request.getHeader(HEADER);
		// Comparação em tempo constante: não revela por timing quantos caracteres acertaram.
		if (recebida == null || !MessageDigest.isEqual(chaveEsperada, recebida.getBytes(StandardCharsets.UTF_8))) {
			throw AcessoNegadoException.chaveInvalida();
		}
		return true;
	}
}
