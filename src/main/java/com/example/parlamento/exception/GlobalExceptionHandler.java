package com.example.parlamento.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.data.core.PropertyReferenceException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.OffsetDateTime;
import java.util.stream.Collectors;

/**
 * Converte toda exceção em {@link StandardError}. Estende o handler do Spring para
 * herdar o tratamento das exceções do próprio MVC (tipo errado, parâmetro ausente,
 * rota inexistente, método não suportado...) e só reescreve o corpo no nosso formato.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@ExceptionHandler(RecursoNaoEncontradoException.class)
	ResponseEntity<StandardError> naoEncontrado(RecursoNaoEncontradoException e, HttpServletRequest request) {
		return responder(HttpStatus.NOT_FOUND, e.getMessage(), request.getRequestURI());
	}

	@ExceptionHandler(ParametroInvalidoException.class)
	ResponseEntity<StandardError> parametroInvalido(ParametroInvalidoException e, HttpServletRequest request) {
		return responder(HttpStatus.BAD_REQUEST, e.getMessage(), request.getRequestURI());
	}

	/** Ordenação por campo inexistente ({@code ?sort=foo}). */
	@ExceptionHandler(PropertyReferenceException.class)
	ResponseEntity<StandardError> ordenacaoInvalida(PropertyReferenceException e, HttpServletRequest request) {
		return responder(HttpStatus.BAD_REQUEST,
				"Não é possível ordenar por '" + e.getPropertyName() + "': campo inexistente",
				request.getRequestURI());
	}

	@ExceptionHandler(FonteExternaIndisponivelException.class)
	ResponseEntity<StandardError> fonteIndisponivel(FonteExternaIndisponivelException e, HttpServletRequest request) {
		log.error("Fonte externa indisponível: {}", e.getMessage(), e);
		return responder(HttpStatus.BAD_GATEWAY,
				e.getMessage() + ". Tente novamente mais tarde.", request.getRequestURI());
	}

	/** Última linha de defesa: registra o stack trace, mas não o expõe ao cliente. */
	@ExceptionHandler(Exception.class)
	ResponseEntity<StandardError> erroInterno(Exception e, HttpServletRequest request) {
		log.error("Erro não tratado em {}", request.getRequestURI(), e);
		return responder(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno inesperado", request.getRequestURI());
	}

	/** Exceções do Spring MVC: mantém o status que o Spring escolheu e reescreve o corpo. */
	@Override
	protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, HttpHeaders headers,
			HttpStatusCode statusCode, WebRequest request) {
		String caminho = request instanceof ServletWebRequest swr ? swr.getRequest().getRequestURI() : null;
		StandardError erro = new StandardError(OffsetDateTime.now(), statusCode.value(),
				descricao(statusCode), mensagem(ex, body), caminho);
		return ResponseEntity.status(statusCode).headers(headers).body(erro);
	}

	private static String mensagem(Exception ex, Object body) {
		return switch (ex) {
			case MethodArgumentTypeMismatchException e -> "Parâmetro '" + e.getName() + "' recebeu '" + e.getValue()
					+ "', mas espera um valor do tipo " + nomeTipo(e.getRequiredType());
			case TypeMismatchException e -> "Valor '" + e.getValue() + "' inválido para o tipo "
					+ nomeTipo(e.getRequiredType());
			case MissingServletRequestParameterException e -> "Parâmetro obrigatório '" + e.getParameterName() + "' ausente";
			case HandlerMethodValidationException e -> e.getParameterValidationResults().stream()
					.flatMap(r -> r.getResolvableErrors().stream()
							.map(err -> r.getMethodParameter().getParameterName() + ": " + err.getDefaultMessage()))
					.collect(Collectors.joining("; "));
			case MethodArgumentNotValidException e -> e.getBindingResult().getFieldErrors().stream()
					.map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
					.collect(Collectors.joining("; "));
			case NoResourceFoundException e -> "Recurso não encontrado: /" + e.getResourcePath();
			case HttpRequestMethodNotSupportedException e -> "Método " + e.getMethod() + " não é suportado neste endpoint";
			default -> body instanceof ProblemDetail pd && pd.getDetail() != null ? pd.getDetail() : "Requisição inválida";
		};
	}

	private static String nomeTipo(Class<?> tipo) {
		if (tipo == null) {
			return "desconhecido";
		}
		return switch (tipo.getSimpleName()) {
			case "Integer", "Long", "int", "long" -> "número inteiro";
			default -> tipo.getSimpleName();
		};
	}

	private static String descricao(HttpStatusCode status) {
		return switch (status.value()) {
			case 400 -> "Requisição inválida";
			case 404 -> "Não encontrado";
			case 405 -> "Método não permitido";
			case 502 -> "Fonte externa indisponível";
			case 500 -> "Erro interno";
			default -> status instanceof HttpStatus hs ? hs.getReasonPhrase() : "Erro";
		};
	}

	private static ResponseEntity<StandardError> responder(HttpStatus status, String mensagem, String caminho) {
		return ResponseEntity.status(status)
				.body(new StandardError(OffsetDateTime.now(), status.value(), descricao(status), mensagem, caminho));
	}
}
