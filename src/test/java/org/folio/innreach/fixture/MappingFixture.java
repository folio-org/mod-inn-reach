package org.folio.innreach.fixture;

import static java.util.UUID.fromString;
import static org.jeasy.random.FieldPredicates.named;

import java.time.OffsetDateTime;
import java.util.UUID;

import lombok.experimental.UtilityClass;
import org.jeasy.random.EasyRandom;
import org.jeasy.random.EasyRandomParameters;
import org.jeasy.random.randomizers.range.IntegerRangeRandomizer;

import org.folio.innreach.domain.entity.CentralServer;
import org.folio.innreach.domain.entity.InnReachLocation;
import org.folio.innreach.domain.entity.LibraryMapping;
import org.folio.innreach.domain.entity.MaterialTypeMapping;

@UtilityClass
public class MappingFixture {

  private static final UUID PRE_POPULATED_CENTRAL_SERVER_UUID = fromString("edab6baf-c696-42b1-89bb-1bbb8759b0d2");
  private static final UUID PRE_POPULATED_IR_LOCATION_UUID = fromString("a1c1472f-67ec-4938-b5a8-f119e51ab79b");

  private static final EasyRandom mtypeRandom;
  private static final EasyRandom libraryRandom;


  static {
    EasyRandomParameters params = new EasyRandomParameters()
        .randomize(named("centralItemType"),new IntegerRangeRandomizer(0, 256))
        .randomize(named("createdBy"), () -> "admin")
        .randomize(named("createdDate"), OffsetDateTime::now)
        .randomize(named("centralServer"), MappingFixture::refCentralServer)
        .excludeField(named("id"))
        .excludeField(named("lastModifiedBy"))
        .excludeField(named("lastModifiedDate"))
        .excludeField(named("metadata"));

    mtypeRandom = new EasyRandom(params);
  }

  static {
    EasyRandomParameters params = new EasyRandomParameters()
        .randomize(named("createdBy"), () -> "admin")
        .randomize(named("createdDate"), OffsetDateTime::now)
        .randomize(named("centralServer"), MappingFixture::refCentralServer)
        .randomize(named("innReachLocation"), MappingFixture::refInnReachLocation)
        .excludeField(named("lastModifiedBy"))
        .excludeField(named("lastModifiedDate"))
        .excludeField(named("metadata"));

    libraryRandom = new EasyRandom(params);
  }

  public static MaterialTypeMapping createMaterialTypeMapping() {
    return mtypeRandom.nextObject(MaterialTypeMapping.class);
  }

  public static LibraryMapping createLibraryMapping() {
    return libraryRandom.nextObject(LibraryMapping.class);
  }

  public static CentralServer refCentralServer() {
    return refCentralServer(PRE_POPULATED_CENTRAL_SERVER_UUID);
  }

  public static CentralServer refCentralServer(UUID id) {
    var centralServer = new CentralServer();
    centralServer.setId(id);
    return centralServer;
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
