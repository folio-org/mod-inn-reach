package org.folio.innreach.fixture;

import static java.util.UUID.fromString;
import static org.jeasy.random.FieldPredicates.named;

import java.time.OffsetDateTime;
import java.util.UUID;

import lombok.experimental.UtilityClass;
import org.jeasy.random.EasyRandom;
import org.jeasy.random.EasyRandomParameters;

import org.folio.innreach.domain.entity.AgencyLocationLscMapping;
import org.folio.innreach.domain.entity.AgencyLocationMapping;
import org.folio.innreach.domain.entity.CentralServer;
import org.folio.innreach.domain.entity.base.AuditableUser;

@UtilityClass
public class AgencyLocationMappingFixture {

  private static final UUID PRE_POPULATED_CENTRAL_SERVER_UUID = fromString("cfae4887-f8fb-4e4c-a5cc-34f74e353cf8");

  private static final EasyRandom agencyLocationRandom;

  static {
    EasyRandomParameters params = new EasyRandomParameters()
      .collectionSizeRange(5, 5)
      .objectPoolSize(50)
      .randomize(named("centralServer"), AgencyLocationMappingFixture::refCentralServer)
      .randomize(named("createdBy"), () -> AuditableUser.SYSTEM)
      .randomize(named("createdDate"), OffsetDateTime::now)
      .excludeField(named("id"))
      .excludeField(named("centralServerMapping"))
      .excludeField(named("localServerMapping"))
      .excludeField(named("updatedBy"))
      .excludeField(named("updatedDate"))
      .excludeField(named("metadata"));

    params.stringLengthRange(5, 5);

    agencyLocationRandom = new EasyRandom(params);
  }

  public static AgencyLocationMapping createMapping() {
    var mapping = agencyLocationRandom.nextObject(AgencyLocationMapping.class);

    var cs = TestUtil.refCentralServer(PRE_POPULATED_CENTRAL_SERVER_UUID);
    mapping.setCentralServer(cs);

    for (var lsm : mapping.getLocalServerMappings()) {
      lsm.setCentralServerMapping(mapping);

      for (var am : lsm.getAgencyCodeMappings()) {
        am.setLocalServerMapping(lsm);
      }
    }

    return mapping;
  }

  public static AgencyLocationLscMapping createLocalServerMapping() {
    var mapping = agencyLocationRandom.nextObject(AgencyLocationLscMapping.class);

    mapping.getAgencyCodeMappings()
      .forEach(acm -> acm.setLocalServerMapping(mapping));

    return mapping;
  }

  public static CentralServer refCentralServer() {
    return TestUtil.refCentralServer(PRE_POPULATED_CENTRAL_SERVER_UUID);
  }

  public static AgencyLocationLscMapping findLocalServerMappingByCode(AgencyLocationMapping mapping, String code) {
    return mapping.getLocalServerMappings()
      .stream()
      .filter(m -> code.equals(m.getLocalServerCode()))
      .findFirst()
      .orElse(null);
  }

}
