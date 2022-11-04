package org.folio.innreach.domain.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.innreach.client.ConfigurationClient;
import org.folio.innreach.domain.dto.folio.ResultList;
import org.folio.innreach.domain.dto.folio.configuration.ConfigurationDTO;
import org.folio.innreach.domain.service.ConfigurationService;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
@RequiredArgsConstructor
@Log4j2
public class ConfigurationServiceImpl implements ConfigurationService {

    private final ConfigurationClient configurationClient;

    @Override
    public ResultList<ConfigurationDTO> fetchConfigurationsDetailsByModule(String module) {
        log.debug("fetchConfigurationsDetailsByModule :: parameter  module : {}", module);
        var  configurationList =  configurationClient.queryRequestByModule(module);
        log.info("fetchConfigurationsDetailsByModule execution ended at " + new Date());
        return configurationList;
    }
}
