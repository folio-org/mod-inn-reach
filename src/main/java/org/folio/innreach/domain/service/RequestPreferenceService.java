package org.folio.innreach.domain.service;

import java.util.UUID;

import org.folio.innreach.domain.dto.folio.requestpreference.RequestPreferenceDTO;

public interface RequestPreferenceService {
  RequestPreferenceDTO findUserRequestPreference(UUID userId);
}
