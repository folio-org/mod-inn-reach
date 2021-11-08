package org.folio.innreach.domain.service.impl;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static org.folio.innreach.domain.dto.folio.ResultList.asSinglePage;

import java.io.IOException;
import java.io.InputStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.folio.innreach.client.CancellationReasonClient;
import org.folio.innreach.client.CancellationReasonClient.CancellationReason;
import org.folio.innreach.client.InstanceContributorTypeClient;
import org.folio.innreach.client.InstanceTypeClient;
import org.folio.innreach.domain.dto.folio.ResultList;
import org.folio.innreach.util.JsonHelper;

@ExtendWith(MockitoExtension.class)
class ReferenceDataLoaderTest {

  @Mock
  private JsonHelper jsonHelper;
  @Mock
  private InstanceContributorTypeClient instanceContributorTypeClient;
  @Mock
  private InstanceTypeClient instanceTypeClient;
  @Mock
  private CancellationReasonClient cancellationReasonClient;

  @InjectMocks
  private ReferenceDataLoader service;

  @Test
  void shouldLoadRefData() throws IOException {
    when(jsonHelper.fromJson(any(InputStream.class), eq(InstanceTypeClient.InstanceType.class)))
      .thenReturn(new InstanceTypeClient.InstanceType());
    when(jsonHelper.fromJson(any(InputStream.class), eq(InstanceContributorTypeClient.NameType.class)))
      .thenReturn(new InstanceContributorTypeClient.NameType());
    when(jsonHelper.fromJson(any(InputStream.class), eq(CancellationReason.class)))
      .thenReturn(new CancellationReason());

    when(instanceTypeClient.queryInstanceTypeByName(any())).thenReturn(ResultList.empty());
    when(instanceContributorTypeClient.queryContributorTypeByName(any())).thenReturn(ResultList.empty());
    when(cancellationReasonClient.queryReasonByName(any())).thenReturn(ResultList.empty());

    service.loadRefData();

    verify(instanceTypeClient).createInstanceType(any());
    verify(instanceContributorTypeClient).createContributorType(any());
    verify(cancellationReasonClient).createReason(any());
  }

  @Test
  void shouldSkipExistingRefData() throws IOException {
    when(jsonHelper.fromJson(any(InputStream.class), eq(InstanceTypeClient.InstanceType.class)))
      .thenReturn(new InstanceTypeClient.InstanceType());
    when(jsonHelper.fromJson(any(InputStream.class), eq(InstanceContributorTypeClient.NameType.class)))
      .thenReturn(new InstanceContributorTypeClient.NameType());
    when(jsonHelper.fromJson(any(InputStream.class), eq(CancellationReason.class)))
      .thenReturn(new CancellationReason());

    when(instanceTypeClient.queryInstanceTypeByName(any())).thenReturn(asSinglePage(new InstanceTypeClient.InstanceType()));
    when(instanceContributorTypeClient.queryContributorTypeByName(any())).thenReturn(asSinglePage(new InstanceContributorTypeClient.NameType()));
    when(cancellationReasonClient.queryReasonByName(any())).thenReturn(asSinglePage(new CancellationReason()));

    service.loadRefData();

    verify(instanceTypeClient, never()).createInstanceType(any());
    verify(instanceContributorTypeClient, never()).createContributorType(any());
    verify(cancellationReasonClient, never()).createReason(any());
  }

}
