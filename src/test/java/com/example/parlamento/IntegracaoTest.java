package com.example.parlamento;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Base dos testes de integração: um PostgreSQL real (Testcontainers) com as
 * migrations do Flyway. O container é um singleton da suíte inteira — com
 * {@code @Container} por classe ele morreria ao fim da primeira classe e o
 * contexto em cache do Spring apontaria para um banco parado.
 */
@SpringBootTest
public abstract class IntegracaoTest {

	@ServiceConnection
	static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine");

	static {
		POSTGRES.start();
	}

	@Autowired
	protected JdbcTemplate jdbc;

	@BeforeEach
	void limparBase() {
		jdbc.execute("TRUNCATE proposicao_autor, proposicao, deputado, partido, ingestao_log RESTART IDENTITY CASCADE");
	}

	protected long contar(String sql, Object... args) {
		Long total = jdbc.queryForObject(sql, Long.class, args);
		return total == null ? 0 : total;
	}
}
