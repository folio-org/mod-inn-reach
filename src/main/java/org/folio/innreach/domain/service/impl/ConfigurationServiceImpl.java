package org.folio.innreach.domain.service.impl;

import java.util.Collections;
import java.util.Map;

import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.folio.innreach.client.CirculationClient;
import org.folio.innreach.client.ConfigurationClient;
import org.folio.innreach.domain.dto.folio.ResultList;
import org.folio.innreach.domain.dto.folio.circulation.CirculationSettingDTO;
import org.folio.innreach.domain.service.ConfigurationService;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Log4j2
public class ConfigurationServiceImpl implements ConfigurationService {

  static final String CHECKOUT_MODULE = "CHECKOUT";

  private final CirculationClient circulationClient;
  private final ConfigurationClient configurationClient;
  private final ObjectMapper objectMapper;

  @Override
  public ResultList<CirculationSettingDTO> fetchCheckoutSettings() {
    log.debug("fetchCheckoutSettings:: trying circulation/settings first");
    var result = circulationClient.getCheckoutSettings();
    if (result != null && CollectionUtils.isNotEmpty(result.getResult())) {
      log.debug("fetchCheckoutSettings:: found settings in circulation/settings");
      return result;
    }

    log.info("fetchCheckoutSettings:: circulation/settings empty, falling back to configuration/entries");
    var legacyResult = configurationClient.queryRequestByModule(CHECKOUT_MODULE);
    if (legacyResult == null || CollectionUtils.isEmpty(legacyResult.getResult())) {
      log.info("fetchCheckoutSettings:: no settings found in either source, using defaults");
      return ResultList.empty();
    }

    var adapted = legacyResult.getResult().stream()
      .map(dto -> new CirculationSettingDTO(dto.getId(), dto.getConfigName(), parseValue(dto.getValue())))
      .toList();

    return ResultList.asSinglePage(adapted);
  }

  private Map<String, Object> parseValue(String jsonValue) {
    if (StringUtils.isBlank(jsonValue)) {
      return Collections.emptyMap();
    }
    try {
      return objectMapper.readValue(jsonValue, new TypeReference<>() {});
    } catch (JacksonException e) {
      log.warn("fetchCheckoutSettings:: failed to parse configuration value, using empty map. Value: [{}], Error: {}",
        jsonValue, e.getMessage());
      return Collections.emptyMap();
    }
  }
}
