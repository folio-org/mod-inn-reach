package org.folio.innreach.domain.service.impl;

import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import org.folio.innreach.client.RequestPreferenceStorageClient;
import org.folio.innreach.domain.dto.folio.requestpreference.RequestPreferenceDTO;
import org.folio.innreach.domain.service.RequestPreferenceService;

@Log4j2
@Service
@RequiredArgsConstructor
public class RequestPreferenceServiceImpl implements RequestPreferenceService {
  private final RequestPreferenceStorageClient client;
  @Override
  public RequestPreferenceDTO findUserRequestPreference(UUID userId) {
    var requestPreferences = client.getUserRequestPreference(userId);
    Assert.isTrue(requestPreferences.getTotalRecords() == 1, "Could not retrieve 1 request preferences" +
      "record for userId = " + userId);

    return requestPreferences.getResult().get(0);
  }
}
