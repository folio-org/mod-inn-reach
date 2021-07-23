package org.folio.innreach.fixture;

import lombok.experimental.UtilityClass;
import org.folio.innreach.domain.entity.AgencyLocationAcMapping;
import org.folio.innreach.domain.entity.AgencyLocationLscMapping;
import org.folio.innreach.domain.entity.AgencyLocationMapping;
import org.jeasy.random.EasyRandom;
import org.jeasy.random.EasyRandomParameters;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

import static java.util.UUID.fromString;
import static org.jeasy.random.FieldPredicates.named;

@UtilityClass
public class AgencyLocationMappingFixture {

  private static final UUID PRE_POPULATED_CENTRAL_SERVER_UUID = fromString("cfae4887-f8fb-4e4c-a5cc-34f74e353cf8");

  private static final EasyRandom agencyLocationRandom;
  private static final EasyRandom agencyLocationCodeRandom;

  static {
    EasyRandomParameters params = new EasyRandomParameters()
      .randomize(named("id"), () -> PRE_POPULATED_CENTRAL_SERVER_UUID)
      .randomize(named("createdBy"), () -> "admin")
      .randomize(named("createdDate"), OffsetDateTime::now)
      .excludeField(named("localServerMappings"))
      .excludeField(named("lastModifiedBy"))
      .excludeField(named("lastModifiedDate"))
      .excludeField(named("metadata"));

    agencyLocationRandom = new EasyRandom(params);
  }

  static {
    EasyRandomParameters params = new EasyRandomParameters()
      .randomize(named("createdBy"), () -> "admin")
      .randomize(named("createdDate"), OffsetDateTime::now)
      .excludeField(named("id"))
      .excludeField(named("centralServerMapping"))
      .excludeField(named("agencyCodeMappings"))
      .excludeField(named("localServerMapping"))
      .excludeField(named("localServerMappings"))
      .excludeField(named("lastModifiedBy"))
      .excludeField(named("lastModifiedDate"))
      .excludeField(named("metadata"));

    params.stringLengthRange(5, 5);

    agencyLocationCodeRandom = new EasyRandom(params);
  }

  public static AgencyLocationMapping createMapping() {
    var mapping = agencyLocationRandom.nextObject(AgencyLocationMapping.class);

    var localServerMappings = createLocalServerMappings();

    mapping.setLocalServerMappings(localServerMappings);
    localServerMappings.forEach(m -> m.setCentralServerMapping(mapping));

    for (var lsm : localServerMappings) {
      var agencyCodeMappings = createAgencyCodeMappings();

      lsm.setAgencyCodeMappings(agencyCodeMappings);
      agencyCodeMappings.forEach(am -> am.setLocalServerMapping(lsm));
    }

    return mapping;
  }

  public static AgencyLocationLscMapping createLocalServerMapping() {
    return agencyLocationCodeRandom.nextObject(AgencyLocationLscMapping.class);
  }

  public static AgencyLocationAcMapping createAgencyCodeMapping() {
    return agencyLocationCodeRandom.nextObject(AgencyLocationAcMapping.class);
  }

  public static Set<AgencyLocationLscMapping> createLocalServerMappings() {
    return listOf(10, AgencyLocationMappingFixture::createLocalServerMapping);
  }

  public static Set<AgencyLocationAcMapping> createAgencyCodeMappings() {
    return listOf(10, AgencyLocationMappingFixture::createAgencyCodeMapping);
  }

  public static AgencyLocationLscMapping findLocalServerMappingByCode(AgencyLocationMapping mapping, String code) {
    return mapping.getLocalServerMappings()
      .stream()
      .filter(m -> code.equals(m.getLocalServerCode()))
      .findFirst()
      .orElse(null);
  }

  private static <T> Set<T> listOf(int bound, Supplier<T> supplier) {
    var total = ThreadLocalRandom.current().nextInt(bound);
    Set<T> list = new HashSet<>(total);
    for (int i = 0; i < total; i++) {
      list.add(supplier.get());
    }
    return list;
  }

}
