package org.folio.innreach.domain.service.impl;

import org.folio.innreach.client.CirculationClient;
import org.folio.innreach.domain.dto.folio.ResultList;
import org.folio.innreach.domain.dto.folio.circulation.CirculationSettingDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.folio.innreach.fixture.TestUtil.deserializeFromJsonFile;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConfigurationServiceImplTest {

  @Mock
  private CirculationClient circulationClient;

  @InjectMocks
  private ConfigurationServiceImpl service;

  @Test
  void shouldGetCheckoutSettings() {
    var circulationSettingDto =
            deserializeFromJsonFile("/configuration/configuration-details-example.json", CirculationSettingDTO.class);
    when(circulationClient.getCheckoutSettings())
            .thenReturn(ResultList.asSinglePage(circulationSettingDto));

    var result = service.fetchCheckoutSettings();

    assertNotNull(result);

    verify(circulationClient).getCheckoutSettings();
  }
}
