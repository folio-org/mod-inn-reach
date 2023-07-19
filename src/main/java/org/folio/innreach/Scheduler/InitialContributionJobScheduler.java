package org.folio.innreach.Scheduler;

import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.innreach.client.AuthnClient;
import org.folio.innreach.client.OkapiClient;
import org.folio.innreach.config.props.SystemUserProperties;
import org.folio.innreach.domain.dto.folio.SystemUser;
import org.folio.innreach.domain.dto.folio.Tenant;
import org.folio.innreach.domain.service.ContributionService;
import org.folio.innreach.domain.service.impl.FolioExecutionContextBuilder;
import org.folio.innreach.domain.service.impl.TenantScopedExecutionService;
import org.folio.innreach.repository.JobExecutionStatusRepository;
import org.folio.spring.integration.XOkapiHeaders;
import org.folio.spring.scope.FolioExecutionContextSetter;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;


import javax.annotation.PostConstruct;

import java.util.ArrayList;
import java.util.List;

import static org.folio.innreach.domain.service.impl.CustomTenantService.tenants;

@Service
@AllArgsConstructor
@Log4j2
public class InitialContributionJobScheduler {
  private TenantScopedExecutionService executionService;
  private JobExecutionStatusRepository jobExecutionStatusRepository;
  private ContributionService contributionService;
  private List<Tenant> tenants;
  private final SystemUserProperties folioSystemUserConf;
  private final AuthnClient authnClient;
  private final SystemUserProperties systemUserConf;
  private final FolioExecutionContextBuilder contextBuilder;
  private final OkapiClient okapiClient;

  @PostConstruct
  public void setTenants() {
    AuthnClient.UserCredentials cred = AuthnClient.UserCredentials
      .of(systemUserConf.getUsername(), folioSystemUserConf.getPassword());

    var response = authnClient.getApiKey(cred);

    List<String> tokenHeaders = response.getHeaders().get(XOkapiHeaders.TOKEN);
    if(tokenHeaders == null || tokenHeaders.isEmpty()){
      log.info("Unable to fetch the tenants list as the token list is empty");
      return;
    }
    var systemUser = new SystemUser();
    systemUser.setToken(tokenHeaders.get(0));
    try (var context = new FolioExecutionContextSetter(contextBuilder.forSystemUser(systemUser))) {
      tenants = okapiClient.getTenantList().getResult();
      log.info("After fetching tenants {} ", tenants);
    }
  }
  @Scheduled(fixedDelay = 30000)
  public void processInitialContributionEvents() {
    log.info("Thread Name {} ", Thread.currentThread().getName());
    log.info("processInitialContributionEvents :: tenantsList {} ", tenants);
    tenants.forEach(tenant ->
      executionService.runTenantScoped(tenant.getId(),
        () -> {
          log.info("Fetching jobs for tenant {} ", tenant);
          jobExecutionStatusRepository.updateAndFetchJobExecutionRecordsByStatus()
            .forEach(contributionService::processInitialContributionEvents);
        }
      ));
  }
}
