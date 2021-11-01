package org.folio.innreach.domain.service.impl;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.folio.innreach.client.InstanceContributorTypeClient;
import org.folio.innreach.client.InstanceTypeClient;
import org.folio.innreach.domain.dto.folio.ResultList;
import org.folio.innreach.util.JsonHelper;

@ExtendWith(MockitoExtension.class)
class ReferenceDataLoaderTest {

  @Mock
  private InstanceContributorTypeClient instanceContributorTypeClient;
  @Mock
  private JsonHelper jsonHelper;
  @Mock
  private InstanceTypeClient instanceTypeClient;

  @InjectMocks
  private ReferenceDataLoader service;

  @Test
  void shouldLoadRefData() throws IOException {
    when(jsonHelper.fromJson(any(InputStream.class), eq(InstanceTypeClient.InstanceType.class)))
      .thenReturn(new InstanceTypeClient.InstanceType());

    when(jsonHelper.fromJson(any(InputStream.class), eq(InstanceContributorTypeClient.NameType.class)))
      .thenReturn(new InstanceContributorTypeClient.NameType());

    when(instanceTypeClient.queryInstanceTypeByName(any())).thenReturn(ResultList.empty());
    when(instanceContributorTypeClient.queryContributorTypeByName(any())).thenReturn(ResultList.empty());

    service.loadRefData();

    verify(instanceTypeClient).createInstanceType(any());
    verify(instanceContributorTypeClient).createContributorType(any());
  }

  @Test
  void shouldSkipExistingInstanceType() throws IOException {
    when(jsonHelper.fromJson(any(InputStream.class), eq(InstanceTypeClient.InstanceType.class)))
      .thenReturn(new InstanceTypeClient.InstanceType());

    when(jsonHelper.fromJson(any(InputStream.class), eq(InstanceContributorTypeClient.NameType.class)))
      .thenReturn(new InstanceContributorTypeClient.NameType());

    when(instanceTypeClient.queryInstanceTypeByName(any())).thenReturn(ResultList.asSinglePage(new InstanceTypeClient.InstanceType()));
    when(instanceContributorTypeClient.queryContributorTypeByName(any())).thenReturn(ResultList.empty());

    service.loadRefData();

    verify(instanceTypeClient, never()).createInstanceType(any());
    verify(instanceContributorTypeClient).createContributorType(any());
  }

  @Test
  void shouldSkipExistingContributorType() throws IOException {
    when(jsonHelper.fromJson(any(InputStream.class), eq(InstanceTypeClient.InstanceType.class)))
      .thenReturn(new InstanceTypeClient.InstanceType());

    when(jsonHelper.fromJson(any(InputStream.class), eq(InstanceContributorTypeClient.NameType.class)))
      .thenReturn(new InstanceContributorTypeClient.NameType());

    when(instanceTypeClient.queryInstanceTypeByName(any())).thenReturn(ResultList.empty());
    when(instanceContributorTypeClient.queryContributorTypeByName(any())).thenReturn(ResultList.asSinglePage(new InstanceContributorTypeClient.NameType()));

    service.loadRefData();

    verify(instanceTypeClient).createInstanceType(any());
    verify(instanceContributorTypeClient, never()).createContributorType(any());
  }
}
