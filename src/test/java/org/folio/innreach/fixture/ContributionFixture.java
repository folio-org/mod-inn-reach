package org.folio.innreach.fixture;

import static java.util.Collections.singletonList;
import static java.util.List.of;
import static java.util.UUID.fromString;
import static org.jeasy.random.FieldPredicates.named;

import static org.folio.innreach.domain.entity.Contribution.Status.IN_PROGRESS;
import static org.folio.innreach.fixture.TestUtil.deserializeFromJsonFile;
import static org.folio.innreach.util.ListUtils.mapItems;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import lombok.experimental.UtilityClass;
import org.jeasy.random.EasyRandom;
import org.jeasy.random.EasyRandomParameters;

import org.folio.innreach.batch.contribution.ContributionJobContext;
import org.folio.innreach.client.InventoryViewClient;
import org.folio.innreach.domain.dto.folio.ResultList;
import org.folio.innreach.domain.dto.folio.inventorystorage.JobResponse;
import org.folio.innreach.domain.dto.folio.inventorystorage.MaterialTypeDTO;
import org.folio.innreach.domain.entity.CentralServer;
import org.folio.innreach.domain.entity.Contribution;
import org.folio.innreach.domain.entity.base.AuditableUser;
import org.folio.innreach.dto.Holding;
import org.folio.innreach.dto.Instance;
import org.folio.innreach.dto.Item;
import org.folio.innreach.dto.TransformedMARCRecordDTO;
import org.folio.innreach.external.dto.InnReachLocationDTO;
import org.folio.innreach.mapper.ContributionMapper;
import org.folio.innreach.mapper.ContributionMapperImpl;
import org.folio.innreach.mapper.MappingMethods;

@UtilityClass
public class ContributionFixture {

  private static final UUID PRE_POPULATED_CENTRAL_SERVER_UUID = fromString("cfae4887-f8fb-4e4c-a5cc-34f74e353cf8");
  private static final String PRE_POPULATED_TYPE_ID = "1a54b431-2e4f-452d-9cae-9cee66c9a892";
  private static final String PRE_POPULATED_TYPE2_ID = "5ee11d91-f7e8-481d-b079-65d708582ccc";
  private static final String PRE_POPULATED_TYPE3_ID = "615b8413-82d5-4203-aa6e-e37984cb5ac3";
  private static final String IR_LOCATION_CODE = "q1w2e";
  private static final String IR_LOCATION2_CODE = "p0o9i";
  private static final String IR_LOCATION3_CODE = "u7y6t";

  private static final EasyRandom contributionRandom;
  private static final EasyRandom instanceRandom;

  public static final ContributionMapper mapper = new ContributionMapperImpl(new MappingMethods());

  static {
    EasyRandomParameters params = new EasyRandomParameters()
      .overrideDefaultInitialization(true)
      .randomize(named("status"), () -> IN_PROGRESS)
      .randomize(named("centralServer"), ContributionFixture::refCentralServer)
      .randomize(named("createdBy"), () -> AuditableUser.SYSTEM)
      .randomize(named("createdDate"), OffsetDateTime::now)
      .excludeField(named("id"))
      .excludeField(named("contribution"))
      .excludeField(named("updatedBy"))
      .excludeField(named("updatedDate"))
      .excludeField(named("metadata"));

    contributionRandom = new EasyRandom(params);
  }

  static {
    EasyRandomParameters params = new EasyRandomParameters()
      .overrideDefaultInitialization(true)
      .objectPoolSize(50)
      .randomize(named("centralServer"), ContributionFixture::refCentralServer);

    instanceRandom = new EasyRandom(params);
  }

  public static Contribution createContribution() {
    var contribution = contributionRandom.nextObject(Contribution.class);

    contribution.getErrors().forEach(e -> e.setContribution(contribution));

    return contribution;
  }

  public static ContributionJobContext createContributionJobContext() {
    return contributionRandom.nextObject(ContributionJobContext.class);
  }

  public static InventoryViewClient.InstanceView createInstanceView() {
    var instanceView = new InventoryViewClient.InstanceView();
    instanceView.setInstance(createInstance());
    instanceView.setItems(of(createItem()));
    instanceView.setHoldingsRecords(of(createHolding()));
    return instanceView;
  }

  public static Instance createInstance() {
    var instance = new Instance();
    instance.setId(UUID.randomUUID());
    instance.setSource("MARC");
    instance.setHrid("test");
    instance.setItems(singletonList(createItem()));
    instance.setHoldings(singletonList(createHolding()));
    return instance;
  }

  public static Item createItem() {
    return instanceRandom.nextObject(Item.class);
  }

  private static Holding createHolding() {
    return instanceRandom.nextObject(Holding.class);
  }

  public static List<InnReachLocationDTO> createIrLocations() {
    return mapItems(Arrays.asList(IR_LOCATION_CODE, IR_LOCATION2_CODE, IR_LOCATION3_CODE), c -> new InnReachLocationDTO(c, null));
  }

  public static ResultList<MaterialTypeDTO> createMaterialTypes() {
    List<MaterialTypeDTO> results = mapItems(Arrays.asList(PRE_POPULATED_TYPE_ID, PRE_POPULATED_TYPE2_ID, PRE_POPULATED_TYPE3_ID),
        ContributionFixture::createMaterialType);

    return ResultList.of(results.size(), results);
  }

  public static MaterialTypeDTO createMaterialType(String id) {
    MaterialTypeDTO dto = new MaterialTypeDTO();
    dto.setId(UUID.fromString(id));
    return dto;
  }

  public static JobResponse createIterationJobResponse() {
    return JobResponse.builder()
      .id(UUID.randomUUID())
      .status(JobResponse.JobStatus.IN_PROGRESS)
      .numberOfRecordsPublished(0)
      .submittedDate(OffsetDateTime.now())
      .build();
  }

  public static TransformedMARCRecordDTO createMARCRecord() {
    return deserializeFromJsonFile("/contribution/marc-record.json", TransformedMARCRecordDTO.class);
  }

  public static CentralServer refCentralServer() {
    return TestUtil.refCentralServer(PRE_POPULATED_CENTRAL_SERVER_UUID);
  }

}
