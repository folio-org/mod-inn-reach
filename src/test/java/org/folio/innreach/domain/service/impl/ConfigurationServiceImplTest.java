package org.folio.innreach.domain.service.impl;

import static org.folio.innreach.domain.service.impl.ConfigurationServiceImpl.CHECKOUT_MODULE;
import static org.folio.innreach.fixture.TestUtil.deserializeFromJsonFile;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.folio.innreach.client.CirculationClient;
import org.folio.innreach.client.ConfigurationClient;
import org.folio.innreach.domain.dto.folio.ResultList;
import org.folio.innreach.domain.dto.folio.circulation.CirculationSettingDTO;
import org.folio.innreach.domain.dto.folio.configuration.ConfigurationDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ConfigurationServiceImplTest {

  @Mock
  private CirculationClient circulationClient;

  @Mock
  private ConfigurationClient configurationClient;

  @Spy
  private ObjectMapper objectMapper = new ObjectMapper();

  @InjectMocks
  private ConfigurationServiceImpl service;

  @Test
  void fetchCheckoutSettings_returnsCirculationSettingsWhenPresent() {
    var dto = deserializeFromJsonFile("/configuration/configuration-details-example.json", CirculationSettingDTO.class);
    when(circulationClient.getCheckoutSettings()).thenReturn(ResultList.asSinglePage(dto));

    var result = service.fetchCheckoutSettings();

    assertNotNull(result);
    assertEquals(1, result.getResult().size());
    verify(circulationClient).getCheckoutSettings();
    verifyNoInteractions(configurationClient);
  }

  @Test
  void fetchCheckoutSettings_fallsBackToConfigurationWhenCirculationEmpty() {
    when(circulationClient.getCheckoutSettings()).thenReturn(ResultList.empty());
    var legacyDto = legacyDto("{\"checkoutTimeoutDuration\":5}");
    when(configurationClient.queryRequestByModule(CHECKOUT_MODULE)).thenReturn(ResultList.asSinglePage(legacyDto));

    var result = service.fetchCheckoutSettings();

    assertEquals(1, result.getResult().size());
    assertEquals(5, result.getResult().get(0).getValue().get("checkoutTimeoutDuration"));
    verify(configurationClient).queryRequestByModule(CHECKOUT_MODULE);
  }

  @Test
  void fetchCheckoutSettings_fallsBackToConfigurationWhenCirculationNull() {
    when(circulationClient.getCheckoutSettings()).thenReturn(null);
    var legacyDto = legacyDto("{\"checkoutTimeoutDuration\":3}");
    when(configurationClient.queryRequestByModule(CHECKOUT_MODULE)).thenReturn(ResultList.asSinglePage(legacyDto));

    var result = service.fetchCheckoutSettings();

    assertEquals(1, result.getResult().size());
    verify(configurationClient).queryRequestByModule(CHECKOUT_MODULE);
  }

  @Test
  void fetchCheckoutSettings_returnsEmptyWhenBothSourcesEmpty() {
    when(circulationClient.getCheckoutSettings()).thenReturn(ResultList.empty());
    when(configurationClient.queryRequestByModule(CHECKOUT_MODULE)).thenReturn(ResultList.empty());

    var result = service.fetchCheckoutSettings();

    assertEquals(0, result.getResult().size());
  }

  @Test
  void fetchCheckoutSettings_returnsEmptyWhenLegacyResultIsNull() {
    when(circulationClient.getCheckoutSettings()).thenReturn(ResultList.empty());
    when(configurationClient.queryRequestByModule(CHECKOUT_MODULE)).thenReturn(null);

    var result = service.fetchCheckoutSettings();

    assertEquals(0, result.getResult().size());
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {"   "})
  void fetchCheckoutSettings_legacyValueNullOrBlank_returnsEmptyValueMap(String value) {
    when(circulationClient.getCheckoutSettings()).thenReturn(ResultList.empty());
    when(configurationClient.queryRequestByModule(CHECKOUT_MODULE))
      .thenReturn(ResultList.asSinglePage(legacyDto(value)));

    var result = service.fetchCheckoutSettings();

    assertEquals(1, result.getResult().size());
    assertEquals(0, result.getResult().get(0).getValue().size());
  }

  @Test
  void fetchCheckoutSettings_legacyValueInvalidJson_returnsEmptyValueMap() {
    when(circulationClient.getCheckoutSettings()).thenReturn(ResultList.empty());
    when(configurationClient.queryRequestByModule(CHECKOUT_MODULE))
      .thenReturn(ResultList.asSinglePage(legacyDto("not-valid-json")));

    var result = service.fetchCheckoutSettings();

    assertEquals(1, result.getResult().size());
    assertEquals(0, result.getResult().get(0).getValue().size());
  }

  private static ConfigurationDTO legacyDto(String value) {
    return new ConfigurationDTO("id", "CHECKOUT", "other_settings", null, null, value);
  }
}
