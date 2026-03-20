package org.folio.innreach.domain.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.innreach.client.CirculationClient;
import org.folio.innreach.domain.dto.folio.ResultList;
import org.folio.innreach.domain.dto.folio.circulation.CirculationSettingDTO;
import org.folio.innreach.domain.service.ConfigurationService;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Log4j2
public class ConfigurationServiceImpl implements ConfigurationService {

    private final CirculationClient circulationClient;

    @Override
    public ResultList<CirculationSettingDTO> fetchCheckoutSettings() {
        log.debug("fetchCheckoutSettings :: fetching CHECKOUT other_settings from circulation/settings");
        return circulationClient.getCheckoutSettings();
    }
}
