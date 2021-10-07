package org.folio.innreach.domain;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public enum InnReachResponseStatus {

  OK("ok");

  private final String responseStatus;
}
