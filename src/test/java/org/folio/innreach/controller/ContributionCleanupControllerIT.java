package org.folio.innreach.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_METHOD;
import static org.springframework.test.context.jdbc.SqlMergeMode.MergeMode.MERGE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;

import org.folio.innreach.it.base.BaseTenantIT;
import org.folio.innreach.repository.JobExecutionStatusRepository;
import org.folio.innreach.repository.OngoingContributionStatusRepository;
import org.folio.innreach.support.TestJdbcHelper;
import org.folio.tenant.domain.dto.TenantAttributes;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlMergeMode;

@SqlMergeMode(MERGE)
@Sql(scripts = {
  "classpath:db/contribution-cleanup/clear-cleanup-tables.sql"
}, executionPhase = AFTER_TEST_METHOD)
class ContributionCleanupControllerIT extends BaseTenantIT {

  private static final String CLEANUP_URL = "/inn-reach/central-servers/contributions/cleanup";
  private static final String PRE_POPULATE_CLEANUP_DATA = "classpath:db/contribution-cleanup/pre-populate-cleanup-test-data.sql";
  private static final String PRE_POPULATE_MULTI_TENANT_DATA = "db/contribution-cleanup/pre-populate-cleanup-multi-tenant-data.sql";
  private static final String SECOND_TENANT = "testing2";

  @Autowired
  private JobExecutionStatusRepository jobRepo;
  @Autowired
  private OngoingContributionStatusRepository ongoingRepo;
  @Autowired
  private TestJdbcHelper testJdbcHelper;

  @BeforeAll
  static void setUpExtraTenant() {
    enableTenant(SECOND_TENANT, new TenantAttributes());
  }

  @AfterAll
  static void tearDownExtraTenant() {
    purgeTenant(SECOND_TENANT);
  }

  @Test
  @Sql(scripts = { PRE_POPULATE_CLEANUP_DATA })
  void cleanup_shouldReturn204() throws Exception {
    mockMvc.perform(post(CLEANUP_URL).headers(defaultHeaders()))
      .andExpect(status().isNoContent());
  }

  @Test
  @Sql(scripts = { PRE_POPULATE_CLEANUP_DATA })
  void cleanup_shouldDeleteTerminalRecordsOlderThanRetention() throws Exception {
    mockMvc.perform(post(CLEANUP_URL).headers(defaultHeaders()))
      .andExpect(status().isNoContent());

    var terminalInitialIds = List.of(
      UUID.fromString("00000000-0000-0000-0000-000000000001"),
      UUID.fromString("00000000-0000-0000-0000-000000000002"),
      UUID.fromString("00000000-0000-0000-0000-000000000003")
    );
    assertThat(jobRepo.findAllById(terminalInitialIds)).isEmpty();

    var terminalOngoingIds = List.of(
      UUID.fromString("00000000-0000-0000-0000-000000000004"),
      UUID.fromString("00000000-0000-0000-0000-000000000005")
    );
    assertThat(ongoingRepo.findAllById(terminalOngoingIds)).isEmpty();
  }

  @Test
  @Sql(scripts = { PRE_POPULATE_CLEANUP_DATA })
  void cleanup_shouldNotDeleteNonTerminalRecords() throws Exception {
    mockMvc.perform(post(CLEANUP_URL).headers(defaultHeaders()))
      .andExpect(status().isNoContent());

    var nonTerminalInitialIds = List.of(
      UUID.fromString("00000000-0000-0000-0000-000000000006"), // IN_PROGRESS
      UUID.fromString("00000000-0000-0000-0000-000000000007"), // READY
      UUID.fromString("00000000-0000-0000-0000-000000000008")  // RETRY
    );
    assertThat(jobRepo.findAllById(nonTerminalInitialIds)).hasSize(3);

    var nonTerminalOngoingIds = List.of(
      UUID.fromString("00000000-0000-0000-0000-000000000009") // IN_PROGRESS
    );
    assertThat(ongoingRepo.findAllById(nonTerminalOngoingIds)).hasSize(1);
  }

  @Test
  @Sql(scripts = { PRE_POPULATE_CLEANUP_DATA })
  void cleanup_shouldNotDeleteRecordsWithinRetention() throws Exception {
    mockMvc.perform(post(CLEANUP_URL).headers(defaultHeaders()))
      .andExpect(status().isNoContent());

    var recentRecordId = UUID.fromString("00000000-0000-0000-0000-000000000010");
    assertThat(jobRepo.findById(recentRecordId)).isPresent();
  }

  @Test
  @Sql(scripts = { "classpath:db/contribution-cleanup/pre-populate-cleanup-extra-testing-data.sql" })
  void cleanup_shouldDeleteTerminalRecordsForAllEnabledTenants() throws Exception {
    testJdbcHelper.executeSqlScript(SECOND_TENANT, PRE_POPULATE_MULTI_TENANT_DATA);

    var secondTenantHeaders = defaultHeaders();
    secondTenantHeaders.set("X-Okapi-Tenant", SECOND_TENANT);

    mockMvc.perform(post(CLEANUP_URL).headers(defaultHeaders()))
      .andExpect(status().isNoContent());
    mockMvc.perform(post(CLEANUP_URL).headers(secondTenantHeaders))
      .andExpect(status().isNoContent());

    var defaultTerminalJobs = testJdbcHelper.countByStatus(TEST_TENANT, "job_execution_status", "PROCESSED")
      + testJdbcHelper.countByStatus(TEST_TENANT, "job_execution_status", "FAILED");
    assertThat(defaultTerminalJobs).isZero();

    var secondTenantJobs = testJdbcHelper.countByStatus(SECOND_TENANT, "job_execution_status", "PROCESSED")
      + testJdbcHelper.countByStatus(SECOND_TENANT, "job_execution_status", "FAILED");
    assertThat(secondTenantJobs).isZero();

    var defaultOngoing = testJdbcHelper.countByStatus(TEST_TENANT, "ongoing_contribution_status", "PROCESSED")
      + testJdbcHelper.countByStatus(TEST_TENANT, "ongoing_contribution_status", "FAILED");
    assertThat(defaultOngoing).isZero();

    var secondTenantOngoing = testJdbcHelper.countByStatus(SECOND_TENANT, "ongoing_contribution_status", "PROCESSED")
      + testJdbcHelper.countByStatus(SECOND_TENANT, "ongoing_contribution_status", "FAILED");
    assertThat(secondTenantOngoing).isZero();

    testJdbcHelper.executeSqlScript(SECOND_TENANT, "db/contribution-cleanup/clear-cleanup-tables.sql");
  }
}
