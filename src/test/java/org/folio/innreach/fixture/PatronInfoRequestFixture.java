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

  public static PatronInfoRequestDTO createPatronInfoRequestWithLastNameFirstName() {
    var patronInfoRequest = new PatronInfoRequestDTO();
    patronInfoRequest.setPatronName("Doe John");
    patronInfoRequest.setVisiblePatronId("111111");
    patronInfoRequest.setPatronAgencyCode("test1");
    return patronInfoRequest;
  }

  public static PatronInfoRequestDTO createPatronInfoRequestWithFirstNameMiddleNameLastName() {
    var patronInfoRequest = new PatronInfoRequestDTO();
    patronInfoRequest.setPatronName("John Paul Doe");
    patronInfoRequest.setVisiblePatronId("111111");
    patronInfoRequest.setPatronAgencyCode("test1");
    return patronInfoRequest;
  }

  public static PatronInfoRequestDTO createPatronInfoRequestWithLastNameFirstNameMiddleName() {
    var patronInfoRequest = new PatronInfoRequestDTO();
    patronInfoRequest.setPatronName("Doe John Paul");
    patronInfoRequest.setVisiblePatronId("111111");
    patronInfoRequest.setPatronAgencyCode("test1");
    return patronInfoRequest;
  }

}
