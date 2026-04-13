package org.folio.innreach.client;

import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

import org.folio.innreach.domain.dto.folio.ResultList;
import org.folio.innreach.domain.dto.folio.requestpreference.RequestPreferenceDTO;

@HttpExchange("request-preference-storage")
public interface RequestPreferenceStorageClient {

    @GetExchange("/request-preference")
    ResultList<RequestPreferenceDTO> findByQuery(@RequestParam("query") String query);
}
