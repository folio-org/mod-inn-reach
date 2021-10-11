package org.folio.innreach.fixture;

import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import lombok.experimental.UtilityClass;

import org.folio.innreach.domain.dto.folio.User;
import org.folio.innreach.domain.dto.folio.patron.PatronBlock;
import org.folio.innreach.dto.PatronInfoResponseDTO;

@UtilityClass
public class PatronFixture {

  public static final UUID USER_ID = UUID.randomUUID();
  public static final String PATRON_FIRST_NAME = "John";
  public static final String PATRON_LAST_NAME = "Doe";
  private static final long expiryDateTs = System.currentTimeMillis();
  public static final String PATRON_BLOCK = "test block";

  public static PatronBlock createPatronBlock() {
    return PatronBlock.builder().blockRequests(true).message(PATRON_BLOCK).build();
  }

  public static User createUser() {
    var user = new User();
    user.setId(USER_ID.toString());
    user.setActive(true);
    user.setExpirationDate(OffsetDateTime.ofInstant(Instant.ofEpochMilli(expiryDateTs), ZoneOffset.UTC));
    user.setPersonal(User.Personal.of(PATRON_FIRST_NAME, null, PATRON_LAST_NAME, null));
    return user;
  }

  public static String getErrorMsg(PatronInfoResponseDTO response) {
    if (isNotEmpty(response.getErrors())) {
      var error = response.getErrors().get(0);
      return isNotEmpty(error.getMessages()) ? error.getMessages().get(0) : null;
    }
    return null;
  }

  public static String getPatronId(User user) {
    return user.getId().replace("-", "");
  }
}
