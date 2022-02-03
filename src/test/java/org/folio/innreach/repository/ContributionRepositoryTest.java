package org.folio.innreach.repository;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import static org.folio.innreach.domain.entity.Contribution.Status.COMPLETE;
import static org.folio.innreach.fixture.ContributionFixture.createContribution;

import java.util.Arrays;
import java.util.UUID;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.jdbc.Sql;

import org.folio.innreach.domain.entity.Contribution;
import org.folio.spring.data.OffsetRequest;

@Sql(scripts = {
  "classpath:db/central-server/pre-populate-central-server.sql",
  "classpath:db/central-server/pre-populate-another-central-server.sql",
  "classpath:db/contribution/pre-populate-contribution.sql"
})
class ContributionRepositoryTest extends BaseRepositoryTest {

  private static final UUID PRE_POPULATED_CS_ID = UUID.fromString("edab6baf-c696-42b1-89bb-1bbb8759b0d2");
  private static final UUID PRE_POPULATED_CURR_CONTRIB_ID = UUID.fromString("ae274737-c398-4cf6-8dd3-d228e5b1f608");
  private static final UUID PRE_POPULATED_HISTORY_CONTRIB1_ID = UUID.fromString("b414ad15-cf4e-40ca-a6be-7e0380dbe96e");
  private static final UUID PRE_POPULATED_HISTORY_CONTRIB2_ID = UUID.fromString("9a344fb9-61bb-49ca-95bd-ad329593671d");

  @Autowired
  private ContributionRepository repository;

  @Test
  void shouldFetchCurrent() {
    var found = repository.fetchCurrentByCentralServerId(PRE_POPULATED_CS_ID).get();

    assertNotNull(found);

    assertEquals(PRE_POPULATED_CURR_CONTRIB_ID, found.getId());
  }

  @Test
  void shouldFetchHistory() {
    var foundPage = repository.fetchHistoryByCentralServerId(PRE_POPULATED_CS_ID, new OffsetRequest(0, 10));

    assertNotNull(foundPage);
    assertEquals(2, foundPage.getTotalElements());

    var found = foundPage.getContent();
    assertEquals(2, found.size());

    Assertions.assertThat(found)
      .extracting(Contribution::getId)
      .containsExactlyInAnyOrderElementsOf(
        Arrays.asList(PRE_POPULATED_HISTORY_CONTRIB1_ID, PRE_POPULATED_HISTORY_CONTRIB2_ID));

    Assertions.assertThat(found)
      .extracting(Contribution::getStatus)
      .allMatch(COMPLETE::equals);
  }

  @Test
  void shouldFetchHistoryOnSecondPage() {
    var foundPage = repository.fetchHistoryByCentralServerId(PRE_POPULATED_CS_ID, new OffsetRequest(1, 1));

    assertNotNull(foundPage);
    assertEquals(2, foundPage.getTotalElements());

    var found = foundPage.getContent();
    assertEquals(1, found.size());
  }

  @Test
  void shouldSaveNewContribution() {
    var newContribution = createContribution();

    var saved = repository.saveAndFlush(newContribution);

    var found = repository.getOne(saved.getId());

    assertNotNull(found);
    assertEquals(newContribution.getId(), found.getId());

    var newErrors = newContribution.getErrors();
    Assertions.assertThat(found.getErrors())
      .hasSize(newErrors.size())
      .usingElementComparatorOnFields("recordId", "message")
      .containsExactlyInAnyOrderElementsOf(newErrors);
  }

  @Test
  void shouldUpdateStatisticsAndErrors() {
    var newContribution = createContribution();
    var contribution = repository.getOne(PRE_POPULATED_CURR_CONTRIB_ID);

    contribution.setRecordsProcessed(newContribution.getRecordsProcessed());
    contribution.setRecordsContributed(newContribution.getRecordsContributed());
    contribution.setRecordsUpdated(newContribution.getRecordsUpdated());
    contribution.setRecordsDecontributed(newContribution.getRecordsDecontributed());

    contribution.getErrors().addAll(newContribution.getErrors());

    repository.saveAndFlush(contribution);

    var updated = repository.getOne(contribution.getId());

    assertEquals(contribution.getRecordsProcessed(), updated.getRecordsProcessed());
    assertEquals(contribution.getRecordsContributed(), updated.getRecordsContributed());
    assertEquals(contribution.getRecordsUpdated(), updated.getRecordsUpdated());
    assertEquals(contribution.getRecordsDecontributed(), updated.getRecordsDecontributed());

    Assertions.assertThat(updated.getErrors())
      .hasSize(contribution.getErrors().size())
      .usingElementComparatorOnFields("recordId", "message")
      .containsExactlyInAnyOrderElementsOf(contribution.getErrors());
  }

  @Test
  void throwExceptionWhenSavingWithoutRecordsTotal() {
    var contribution = createContribution();
    contribution.setRecordsTotal(null);

    assertThrows(DataIntegrityViolationException.class, () -> repository.saveAndFlush(contribution));
  }

  @Test
  void throwExceptionWhenSavingWithInvalidCentralServerId() {
    var contribution = createContribution();
    contribution.getCentralServer().setId(randomUUID());

    var ex = assertThrows(DataIntegrityViolationException.class, () -> repository.saveAndFlush(contribution));
    assertThat(ex.getMessage(), containsString("constraint [fk_contribution_cs_id]"));
  }

}
