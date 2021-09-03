package org.folio.innreach.fixture;

import static java.util.UUID.fromString;
import static org.jeasy.random.FieldPredicates.named;

import java.time.OffsetDateTime;
import java.util.UUID;

import lombok.experimental.UtilityClass;
import org.jeasy.random.EasyRandom;
import org.jeasy.random.EasyRandomParameters;
import org.jeasy.random.randomizers.range.IntegerRangeRandomizer;
import org.jeasy.random.randomizers.text.StringRandomizer;

import org.folio.innreach.domain.entity.CentralServer;
import org.folio.innreach.domain.entity.InnReachLocation;
import org.folio.innreach.domain.entity.InnReachPatronTypeMapping;
import org.folio.innreach.domain.entity.ItemTypeMapping;
import org.folio.innreach.domain.entity.LibraryMapping;
import org.folio.innreach.domain.entity.LocationMapping;
import org.folio.innreach.domain.entity.MaterialTypeMapping;
import org.folio.innreach.domain.entity.PatronTypeMapping;
import org.folio.innreach.domain.entity.UserCustomFieldMapping;
import org.folio.innreach.domain.entity.base.AuditableUser;

@UtilityClass
public class MappingFixture {

  private static final UUID PRE_POPULATED_CENTRAL_SERVER_UUID = fromString("edab6baf-c696-42b1-89bb-1bbb8759b0d2");
  private static final UUID PRE_POPULATED_IR_LOCATION_UUID = fromString("a1c1472f-67ec-4938-b5a8-f119e51ab79b");

  private static final EasyRandom mtypeRandom;
  private static final EasyRandom libraryAndLocationRandom;
  private static final EasyRandom patronTypeRandom;
  private static final EasyRandom itemTypeRandom;
  private static final EasyRandom userCustomFieldRandom;
  private static final EasyRandom innReachPatronRandom;

  static {
    EasyRandomParameters params = new EasyRandomParameters()
        .randomize(named("centralItemType"),new IntegerRangeRandomizer(0, 256))
        .randomize(named("createdBy"), () -> AuditableUser.SYSTEM)
        .randomize(named("createdDate"), OffsetDateTime::now)
        .randomize(named("centralServer"), MappingFixture::refCentralServer)
        .excludeField(named("id"))
        .excludeField(named("updatedBy"))
        .excludeField(named("updatedDate"))
        .excludeField(named("metadata"));

    mtypeRandom = new EasyRandom(params);
  }

  static {
    EasyRandomParameters params = new EasyRandomParameters()
        .randomize(named("createdBy"), () -> AuditableUser.SYSTEM)
        .randomize(named("createdDate"), OffsetDateTime::now)
        .randomize(named("centralServer"), MappingFixture::refCentralServer)
        .randomize(named("innReachLocation"), MappingFixture::refInnReachLocation)
        .excludeField(named("updatedBy"))
        .excludeField(named("updatedDate"))
        .excludeField(named("metadata"));

    libraryAndLocationRandom = new EasyRandom(params);
  }

  static {
    EasyRandomParameters params = new EasyRandomParameters()
      .randomize(named("patronType"), new IntegerRangeRandomizer(0, 256))
      .randomize(named("createdBy"), () -> AuditableUser.SYSTEM)
      .randomize(named("createdDate"), OffsetDateTime::now)
      .randomize(named("centralServer"), MappingFixture::refCentralServer)
      .excludeField(named("id"))
      .excludeField(named("updatedBy"))
      .excludeField(named("updatedDate"))
      .excludeField(named("metadata"));

    patronTypeRandom = new EasyRandom(params);
  }

  static {
    EasyRandomParameters params = new EasyRandomParameters()
      .randomize(named("centralItemType"), new IntegerRangeRandomizer(0, 256))
      .randomize(named("createdBy"), () -> AuditableUser.SYSTEM)
      .randomize(named("createdDate"), OffsetDateTime::now)
      .randomize(named("centralServer"), MappingFixture::refCentralServer)
      .excludeField(named("id"))
      .excludeField(named("updatedBy"))
      .excludeField(named("updatedDate"))
      .excludeField(named("metadata"));

    itemTypeRandom = new EasyRandom(params);
  }

  static {
    EasyRandomParameters params = new EasyRandomParameters()
      .randomize(named("agencyCode"), TestUtil::randomFiveCharacterCode)
      .randomize(named("createdBy"), () -> AuditableUser.SYSTEM)
      .randomize(named("createdDate"), OffsetDateTime::now)
      .randomize(named("centralServer"), MappingFixture::refCentralServer)
      .excludeField(named("id"))
      .excludeField(named("updatedBy"))
      .excludeField(named("updatedDate"))
      .excludeField(named("metadata"));

    userCustomFieldRandom = new EasyRandom(params);
  }

  static {
    EasyRandomParameters params = new EasyRandomParameters()
      .randomize(named("innReachPatronType"), new IntegerRangeRandomizer(0, 256))
      .randomize(named("folioUserBarcode"), new StringRandomizer(10))
      .randomize(named("createdBy"), () -> AuditableUser.SYSTEM)
      .randomize(named("createdDate"), OffsetDateTime::now)
      .randomize(named("centralServer"), MappingFixture::refCentralServer)
      .excludeField(named("id"))
      .excludeField(named("updatedBy"))
      .excludeField(named("updatedDate"))
      .excludeField(named("metadata"));

    innReachPatronRandom = new EasyRandom(params);
  }

  public static MaterialTypeMapping createMaterialTypeMapping() {
    return mtypeRandom.nextObject(MaterialTypeMapping.class);
  }

  public static LibraryMapping createLibraryMapping() {
    return libraryAndLocationRandom.nextObject(LibraryMapping.class);
  }

  public static LocationMapping createLocationMapping() {
    return libraryAndLocationRandom.nextObject(LocationMapping.class);
  }

  public static PatronTypeMapping createPatronTypeMapping() {
    return patronTypeRandom.nextObject(PatronTypeMapping.class);
  }

  public static UserCustomFieldMapping createUserCustomFieldMapping() {
    return userCustomFieldRandom.nextObject(UserCustomFieldMapping.class);
  }

  public static ItemTypeMapping createItemTypeMapping() {
    return itemTypeRandom.nextObject(ItemTypeMapping.class);
  }

  public static InnReachPatronTypeMapping createInnReachPatronTypeMapping() {
    return innReachPatronRandom.nextObject(InnReachPatronTypeMapping.class);
  }

  public static CentralServer refCentralServer() {
    return TestUtil.refCentralServer(PRE_POPULATED_CENTRAL_SERVER_UUID);
  }

  public static InnReachLocation refInnReachLocation() {
    return refInnReachLocation(PRE_POPULATED_IR_LOCATION_UUID);
  }

  public static InnReachLocation refInnReachLocation(UUID id) {
    var location = new InnReachLocation();
    location.setId(id);
    return location;
  }

}
