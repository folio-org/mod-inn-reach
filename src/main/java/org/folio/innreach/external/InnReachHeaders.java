package org.folio.innreach.external;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class InnReachHeaders {
  public static final String X_FROM_CODE = "X-From-Code";
  public static final String X_TO_CODE = "X-To-Code";
  public static final String X_D2IR_AUTHORIZATION = "X-D2IR-Authorization";
}
