package org.folio.innreach.fixture;

import lombok.experimental.UtilityClass;
import org.folio.innreach.dto.PatronInfoRequestDTO;

@UtilityClass
public class PatronInfoRequestFixture {
  public static PatronInfoRequestDTO createPatronInfoRequest() {
    var patronInfoRequest = new PatronInfoRequestDTO();
    patronInfoRequest.setPatronName("John Doe");
    patronInfoRequest.setVisiblePatronId("111111");
    patronInfoRequest.setPatronAgencyCode("test1");
    return patronInfoRequest;
  }
}
