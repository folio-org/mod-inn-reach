package org.folio.innreach.support;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.folio.spring.FolioModuleMetadata;
import org.folio.spring.scope.FolioExecutionContextSetter;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.support.AbstractTestExecutionListener;

/**
 * Sets FolioExecutionContext with the test tenant before each test method,
 * ensuring that DataSourceFolioWrapper sets the correct search_path for
 * {@code @Sql} scripts and JPA queries.
 *
 * <p>Runs at order 4999, just before {@code SqlScriptsTestExecutionListener} (5000),
 * so the context is available when @Sql scripts execute.
 */
public class FolioContextTestExecutionListener extends AbstractTestExecutionListener {

  private static final String SETTER_ATTR = "folioContextSetter";
  private static final String TEST_TENANT = "testing";

  @Override
  public int getOrder() {
    return 4999;
  }

  @Override
  public void beforeTestMethod(TestContext testContext) {
    var metadata = testContext.getApplicationContext().getBean(FolioModuleMetadata.class);
    Map<String, Collection<String>> headers = Map.of(
      "x-okapi-tenant", List.of(TEST_TENANT)
    );
    var setter = new FolioExecutionContextSetter(metadata, headers);
    testContext.setAttribute(SETTER_ATTR, setter);
  }

  @Override
  public void afterTestMethod(TestContext testContext) {
    var setter = testContext.getAttribute(SETTER_ATTR);
    if (setter instanceof FolioExecutionContextSetter fecs) {
      fecs.close();
      testContext.removeAttribute(SETTER_ATTR);
    }
  }
}
