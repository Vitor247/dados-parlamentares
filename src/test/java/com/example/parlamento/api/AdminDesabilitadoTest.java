package com.example.parlamento.api;

import com.example.parlamento.IntegracaoTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import static org.assertj.core.api.Assertions.assertThat;

/** Servidor sem ADMIN_API_KEY: a administração fica bloqueada, nunca aberta. */
@AutoConfigureMockMvc
class AdminDesabilitadoTest extends IntegracaoTest {

	@Autowired
	private MockMvcTester mvc;

	@Test
	void semChaveConfiguradaAdministracaoE403MesmoComHeader() {
		assertThat(mvc.post().uri("/api/v1/admin/ingestao/base").header("X-Admin-Key", ""))
				.hasStatus(HttpStatus.FORBIDDEN)
				.bodyJson().extractingPath("$.mensagem").asString().contains("ADMIN_API_KEY");
		assertThat(mvc.post().uri("/api/v1/admin/ingestao/base").header("X-Admin-Key", "qualquer-coisa"))
				.hasStatus(HttpStatus.FORBIDDEN);
	}
}
