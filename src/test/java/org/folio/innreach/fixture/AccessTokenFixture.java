package org.folio.innreach.fixture;

import static org.folio.innreach.fixture.TestUtil.randomUUIDString;

import org.folio.innreach.external.dto.AccessTokenDTO;
import org.folio.innreach.external.dto.AccessTokenRequestDTO;

public class AccessTokenFixture {

  public static AccessTokenDTO createAccessToken() {
    return new AccessTokenDTO(randomUUIDString(), "Bearer", 599);
  }

  public static AccessTokenRequestDTO createAccessTokenRequest() {
    return new AccessTokenRequestDTO("http://centralserver", randomUUIDString(), randomUUIDString());
  }
}
