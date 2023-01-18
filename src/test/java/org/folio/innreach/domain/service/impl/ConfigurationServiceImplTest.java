package org.folio.innreach.domain.service.impl;

import org.folio.innreach.client.ConfigurationClient;
import org.folio.innreach.domain.dto.folio.ResultList;
import org.folio.innreach.domain.dto.folio.configuration.ConfigurationDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.folio.innreach.fixture.TestUtil.deserializeFromJsonFile;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConfigurationServiceImplTest {

  @Mock
  private ConfigurationClient configurationClient;

  @InjectMocks
  private ConfigurationServiceImpl service;

  @Test
  void shouldGetInstanceById() {
    var configurationDto =
            deserializeFromJsonFile("/configuration/configuration-details-example.json", ConfigurationDTO.class);
    when(service.fetchConfigurationsDetailsByModule(any())).
            thenReturn(ResultList.asSinglePage(configurationDto));

    var instance = service.fetchConfigurationsDetailsByModule(any());

    assertNotNull(instance);

    verify(configurationClient).queryRequestByModule(any());
  }
}
