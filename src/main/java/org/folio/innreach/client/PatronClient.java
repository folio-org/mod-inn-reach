package org.folio.innreach.client;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import java.util.UUID;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

import org.folio.innreach.domain.dto.folio.patron.PatronDTO;

@HttpExchange("patron")
public interface PatronClient {

  @GetExchange(value = "/account/{id}", accept = APPLICATION_JSON_VALUE)
  PatronDTO getAccountDetails(@PathVariable("id") UUID userId);

}
