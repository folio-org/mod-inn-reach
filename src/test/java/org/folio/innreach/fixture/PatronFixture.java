package org.folio.innreach.fixture;

import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import lombok.experimental.UtilityClass;

import org.folio.innreach.client.AutomatedPatronBlocksClient.AutomatedPatronBlock;
import org.folio.innreach.client.ManualPatronBlocksClient.ManualPatronBlock;
import org.folio.innreach.domain.dto.folio.User;
import org.folio.innreach.dto.PatronInfoResponseDTO;
import org.folio.innreach.dto.UserCustomFieldMappingDTO;
import org.folio.innreach.util.UUIDEncoder;

@UtilityClass
public class PatronFixture {

  public static final String CUSTOM_FIELD_REF_ID = "homeLibrary";
  public static final String CUSTOM_FIELD_OPTION = "opt_0";
  public static final String CENTRAL_AGENCY_CODE = "code1";
  public static final UUID USER_ID = UUID.randomUUID();
  public static final String PATRON_FIRST_NAME = "John";
  public static final String PATRON_LAST_NAME = "Doe";
  private static final long expiryDateTs = System.currentTimeMillis();
  public static final String PATRON_BLOCK_DESC = "test block desc";
  public static final String PATRON_BLOCK = "test block";
  private static final String PATRON_MIDDLE_NAME = "Paul";

  public static AutomatedPatronBlock createAutomatedPatronBlock() {
    return AutomatedPatronBlock.builder().blockRequests(true).message(PATRON_BLOCK).build();
  }

  public static ManualPatronBlock createManualPatronBlock() {
    return ManualPatronBlock.builder().requests(true).desc(PATRON_BLOCK_DESC).patronMessage(PATRON_BLOCK).build();
  }

  public static User createUser() {
    var user = new User();
    user.setId(USER_ID);
    user.setActive(true);
    user.setExpirationDate(OffsetDateTime.ofInstant(Instant.ofEpochMilli(expiryDateTs), ZoneOffset.UTC));
    user.setPersonal(User.Personal.of(PATRON_FIRST_NAME, null, PATRON_LAST_NAME, null));
    user.setCustomFields(Map.of(CUSTOM_FIELD_REF_ID, CUSTOM_FIELD_OPTION));
    return user;
  }

  public static User createUserWithoutExpirationDate() {
    var user = new User();
    user.setId(USER_ID);
    user.setActive(true);
    user.setPersonal(User.Personal.of(PATRON_FIRST_NAME, null, PATRON_LAST_NAME, null));
    user.setCustomFields(Map.of(CUSTOM_FIELD_REF_ID, CUSTOM_FIELD_OPTION));
    return user;
  }

  public static User createUserWithMiddleName() {
    var user = new User();
    user.setId(USER_ID);
    user.setActive(true);
    user.setExpirationDate(OffsetDateTime.ofInstant(Instant.ofEpochMilli(expiryDateTs), ZoneOffset.UTC));
    user.setPersonal(User.Personal.of(PATRON_FIRST_NAME, PATRON_MIDDLE_NAME, PATRON_LAST_NAME, null));
    user.setCustomFields(Map.of(CUSTOM_FIELD_REF_ID, CUSTOM_FIELD_OPTION));
    return user;
  }

  public static User createUserWithTwoFirstAndTwoLastNames(String firstName, String lastName) {
    var user = new User();
    user.setId(USER_ID);
    user.setActive(true);
    user.setExpirationDate(OffsetDateTime.ofInstant(Instant.ofEpochMilli(expiryDateTs), ZoneOffset.UTC));
    user.setPersonal(User.Personal.of(firstName, PATRON_MIDDLE_NAME, lastName, null));
    user.setCustomFields(Map.of(CUSTOM_FIELD_REF_ID, CUSTOM_FIELD_OPTION));
    return user;
  }

  public static UserCustomFieldMappingDTO createCustomFieldMapping() {
    var mapping = new UserCustomFieldMappingDTO();
    mapping.setConfiguredOptions(Map.of(CUSTOM_FIELD_OPTION, CENTRAL_AGENCY_CODE));
    mapping.setId(UUID.randomUUID());
    mapping.setCustomFieldId(CUSTOM_FIELD_REF_ID);
    return mapping;
  }

  public static String getErrorMsg(PatronInfoResponseDTO response) {
    if (isNotEmpty(response.getErrors())) {
      var error = response.getErrors().get(0);
      return isNotEmpty(error.getMessages()) ? error.getMessages().get(0) : null;
    }
    return null;
  }

  public static String getPatronId(User user) {
    return UUIDEncoder.encode(user.getId());
  }

  public static User createUserWithNonStringCustomFieldValues() {
    var user = new User();
    user.setId(UUID.randomUUID());
    user.setActive(true);
    user.setExpirationDate(OffsetDateTime.now().plusYears(1));
    user.setPersonal(User.Personal.of(PATRON_FIRST_NAME, null, PATRON_LAST_NAME, null));

    // Include a custom field with a non-string value (array)
    Map<String, Object> customFields = new HashMap<>();
    customFields.put(CUSTOM_FIELD_REF_ID, CUSTOM_FIELD_OPTION);
    customFields.put("arrayField", new String[]{"value2", "value3"});

    Map<String, String> customFieldsWithOnlyStringValues = customFields.entrySet().stream()
      .filter(entry -> entry.getValue() instanceof String)
      .collect(Collectors.toMap(Map.Entry::getKey, entry -> (String) entry.getValue()));

    user.setCustomFields(customFieldsWithOnlyStringValues);

    return user;
  }

}
