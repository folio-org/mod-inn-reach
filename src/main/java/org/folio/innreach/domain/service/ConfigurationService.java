package org.folio.innreach.domain.service;

import org.folio.innreach.domain.dto.folio.ResultList;
import org.folio.innreach.domain.dto.folio.circulation.CirculationSettingDTO;
import org.springframework.stereotype.Service;

@Service
public interface ConfigurationService {

    ResultList<CirculationSettingDTO> fetchCheckoutSettings();
}
