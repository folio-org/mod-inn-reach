# Integration Test Infrastructure Unification

## Problem

mod-inn-reach has 4 separate base test classes with fragmented infrastructure:

| Base Class | Subclasses | PostgreSQL | Kafka | WireMock | Profiles | HTTP Client |
|---|---|---|---|---|---|---|
| `BaseControllerTest` | 26 | `BeanFactoryPostProcessor` (System.setProperty) | Mocked (`@MockitoBean`) | None | `test`, `it` | TestRestTemplate |
| `BaseApiControllerTest` | 8 | Same `BeanFactoryPostProcessor` | Mocked (`@MockitoBean`) | Embedded WireMockServer | `test`, `it` | MockMvc |
| `BaseKafkaApiTest` | 5 | `@Container` + `@DynamicPropertySource` | `@EmbeddedKafka` (Spring) | None | `test` | N/A |
| `BaseRepositoryTest` | 18 | `@Container` + `@DynamicPropertySource` | None (`@DataJpaTest`) | None | `test` | N/A |

Plus 2 standalone ITs (`TenantInitIT`, `TenantInitWithSystemUserEnabledIT`) with their own WireMock setup.

Key inconsistencies:
- 3 different PostgreSQL container provisioning strategies
- 2 profiles (`test` and `it`) with overlapping YAML configs
- WireMock setup duplicated between `BaseApiControllerTest` and standalone ITs
- Kafka listeners mocked in 2 base classes (duplicated `@MockitoBean` fields)
- Split between TestRestTemplate and MockMvc for HTTP testing

## Goal

Unify all `@SpringBootTest`-based integration tests under a single base class hierarchy following the mod-dcb pattern:
- One profile, one database, one Kafka container, one WireMock container
- Consistent singleton containers via JUnit 5 extensions
- All tests use MockMvc
- Real tenant initialization via `enableTenant()` (no `TestTenantController` stub)

`BaseRepositoryTest` (18 `@DataJpaTest` subclasses) stays separate but reuses the PostgreSQL container extension.

## Reference Implementation

The design follows `mod-dcb`'s integration test pattern:
- `org.folio.dcb.it.base.BaseIntegrationTest`
- `org.folio.dcb.it.base.BaseTenantIntegrationTest`
- `org.folio.dcb.support.{kafka,postgres,wiremock}.*` container extensions

## Design

### 1. Container Extensions (JUnit 5 Extensions)

All extensions live under `src/test/java/org/folio/innreach/support/`.

#### PostgresContainerExtension + @WithPostgresContainer

Package: `org.folio.innreach.support.postgres`

- Implements `BeforeAllCallback`, `AfterAllCallback`
- Singleton `PostgreSQLContainer` with `postgres:16-alpine` (or `TESTCONTAINERS_POSTGRES_IMAGE` env var)
- `beforeAll()`: starts container if not running, sets system properties:
  - `spring.datasource.url` -> `container.getJdbcUrl()`
  - `spring.datasource.username` -> `container.getUsername()`
  - `spring.datasource.password` -> `container.getPassword()`
- `afterAll()`: clears system properties
- `@WithPostgresContainer`: annotation that triggers `@ExtendWith(PostgresContainerExtension.class)`

#### KafkaContainerExtension + @WithKafkaContainer

Package: `org.folio.innreach.support.kafka`

- Implements `BeforeAllCallback`, `AfterAllCallback`
- Singleton `KafkaContainer` with `apache/kafka-native:3.8.0`, auto-create-topics disabled, 3 startup attempts
- `beforeAll()`: starts container if not running, sets system property:
  - `spring.kafka.bootstrap-servers` -> `container.getBootstrapServers()`
- `afterAll()`: clears system property
- Static helpers:
  - `createTopics(List<String>)` -- creates topics via `KafkaAdminClient`
  - `deleteTopics(List<String>)` -- deletes topics via `KafkaAdminClient`
  - `getAdminClient()` -- returns `AdminClient` for direct access
  - `getBootstrapServers()` -- returns bootstrap servers string
- `@WithKafkaContainer`: annotation that triggers `@ExtendWith(KafkaContainerExtension.class)`

New Maven dependency required:
```xml
<dependency>
  <groupId>org.testcontainers</groupId>
  <artifactId>kafka</artifactId>
  <version>${testcontainers.version}</version>
  <scope>test</scope>
</dependency>
```

#### WiremockContainerExtension + WiremockStubExtension + @WithWiremockContainer + @WireMockStub

Package: `org.folio.innreach.support.wiremock`

**WiremockContainerExtension:**
- Implements `BeforeAllCallback`, `AfterAllCallback`
- Singleton `GenericContainer` with `wiremock/wiremock:3.13.2`:
  - Shared network, exposed port 8080
  - `--local-response-templating`, `--disable-banner` command flags
  - Copies `wm/__files/` from classpath into `/home/wiremock/__files` in the container (so existing response body files work)
  - Slf4j log consumer
- `beforeAll()`: starts container if not running, creates `HttpAdminClient` and `WireMock` client, sets system properties:
  - `wm.url` -> container URL
  - `folio.okapi-url` -> container URL
- `afterAll()`: clears system properties
- Static accessors: `getWireMockAdminClient()`, `getWireMockClient()`

**WiremockStubExtension:**
- Implements `BeforeEachCallback`, `AfterEachCallback`, `BeforeAllCallback`, `AfterAllCallback`
- `beforeAll()` / `afterAll()`: resets all WireMock stubs
- `beforeEach()`: reads `@WireMockStub` annotations from class and method, loads JSON stub files from classpath, registers them with WireMock via Admin API
- `afterEach()`: validates no unmatched requests, validates no unused stubs, then resets
- Static `addStubMappings(String... paths)` for programmatic stub loading
- Static `resetWiremockStubs()` for manual reset
- Supports both single stub JSON and arrays of mappings

**@WithWiremockContainer:**
- Triggers `@ExtendWith({WiremockContainerExtension.class, WiremockStubExtension.class})`

**@WireMockStub:**
- Target: `METHOD`, `TYPE`
- Attribute: `String[] value()` -- classpath paths to stub JSON files

### 2. Base Class Hierarchy

Package: `org.folio.innreach.it.base`

#### BaseIntegrationTest

```java
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@WithPostgresContainer
@WithKafkaContainer
@WithWiremockContainer
@SqlMergeMode(MERGE)
public abstract class BaseIntegrationTest {

  protected static final String MODULE_NAME = "mod-inn-reach";
  protected static final String TEST_TENANT = "testing";
  protected static final String TEST_TOKEN = "dGVzdF9qd3RfdG9rZW4=";

  protected static MockMvc mockMvc;

  @BeforeAll
  static void setupMockMvc(@Autowired MockMvc mockMvc) {
    BaseIntegrationTest.mockMvc = mockMvc;
  }

  protected static void enableTenant() { ... }
  protected static void enableTenant(String tenant, TenantAttributes attrs) { ... }
  protected static void purgeTenant() { ... }
  protected static void purgeTenant(String tenantId) { ... }
  protected static HttpHeaders defaultHeaders() { ... }
  protected static HttpHeaders defaultHeaders(String userId) { ... }
  protected static String getWiremockUrl() { ... }
}
```

- No `TestTenantController` -- real `TenantController` from folio-spring-base handles `/_/tenant` (Liquibase runs on tenant init)
- No `@MockitoBean` declarations -- each test class declares its own
- Only `TenantInitIT` and `TenantInitWithSystemUserEnabledIT` extend this directly

#### BaseTenantIntegrationTest

```java
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public abstract class BaseTenantIntegrationTest extends BaseIntegrationTest {

  protected static final List<String> TENANT_TOPICS = List.of(
    "folio.testing.circulation.loan",
    "folio.testing.circulation.request",
    "folio.testing.circulation.check-in",
    "folio.testing.inventory.item",
    "folio.testing.inventory.holdings-record",
    "folio.testing.inventory.instance",
    "folio.testing.inventory.instance-contribution"
  );

  protected static WireMock wiremock;

  @BeforeAll
  static void beforeAll() {
    createTopics(TENANT_TOPICS);
    setUpMockForTestTenantInit();  // WireMock stubs needed for tenant init
    enableTenant();
    resetWiremockStubs();
    wiremock = getWireMockClient();
  }

  @AfterAll
  static void afterAll() {
    resetWiremockStubs();
    purgeTenant();
    deleteTopics(TENANT_TOPICS);
    wiremock = null;
  }

  // WireMock helper methods migrated from BaseApiControllerTest:
  // stubGet(), stubPost(), stubPut(), stubDelete()
  // Template, URI, MappingActions, ResponseActions inner classes
  // getOkapiHeaders(), awaitUntilAsserted()
}
```

- All ~39 test classes (except the 2 TenantInit ITs) extend this
- `@DirtiesContext(AFTER_CLASS)` ensures each test class gets a fresh context
- No global `@Sql` cleanup in the base class -- each test class keeps its own existing `@Sql(executionPhase = AFTER_TEST_METHOD)` cleanup annotations, which merge with the base via `@SqlMergeMode(MERGE)`
- Kafka topics for all INN-Reach domain events created/deleted around tenant lifecycle
- Multi-tenant topics (`testing1`, `testing4`): Kafka listener tests that need additional tenants create extra topics in their own `@BeforeAll` via `KafkaContainerExtension.createTopics()` and clean them up in `@AfterAll`

### 3. TestTenantScopedExecutionService

Standalone class at `org.folio.innreach.support.TestTenantScopedExecutionService`:

```java
@Primary
@Service
@Profile("test")
public class TestTenantScopedExecutionService extends TenantScopedExecutionService {
  // Runs jobs synchronously (no tenant scoping)
  // Throws RuntimeException for tenant "testing4" (error scenario testing)
}
```

Extracted from `BaseKafkaApiTest` inner class. Available to all test classes via component scanning.

### 4. Profile and Configuration Consolidation

**Single `test` profile.** `application-test.yml` becomes the sole test config.

Changes to `application-test.yml`:
- Remove `spring.kafka.bootstrap-servers: ${spring.embedded.kafka.brokers}` -- KafkaContainerExtension sets it via System.setProperty
- Remove the `spring.datasource` block entirely (url, username, password) -- PostgresContainerExtension sets these via System.setProperty, which takes precedence over YAML. No YAML placeholders needed.
- Keep all other settings: Kafka producer/consumer serializers, tenant settings, logging, cache config, etc.

**Files to delete:**
- `src/test/resources/application-it.yml`
- `src/test/java/org/folio/innreach/context/PostgresTestContainersBeanFactoryPostProcessor.java`

**`spring.properties`** stays as-is.

### 5. WireMock Stub Migration Strategy

**Phase 1 (this refactoring):**
- Copy `wm/__files/` into the WireMock container via `withCopyToContainer()` so existing response body files work
- Migrate `BaseApiControllerTest` helper methods (`stubGet`, `stubPost`, etc.) to `BaseTenantIntegrationTest`
- Existing programmatic stubs continue working via the WireMock Admin API
- The `wm/mappings/d2ir/oauth-mapping.json` loaded via `WiremockStubExtension.addStubMappings()` in `setUpMockForTestTenantInit()`

**Phase 2 (future, incremental):**
- New stubs go into `src/test/resources/stubs/` as self-contained JSON files
- Existing tests gradually adopt `@WireMockStub` annotation-driven approach when touched for other reasons

### 6. Test Class Migration

#### A. 26 BaseControllerTest subclasses (TestRestTemplate -> MockMvc)

1. Change `extends BaseControllerTest` to `extends BaseTenantIntegrationTest`
2. Remove `@Autowired TestRestTemplate testRestTemplate`
3. Convert HTTP calls:
   - `testRestTemplate.postForEntity(url, body, Type.class)` -> `mockMvc.perform(post(url).content(asJsonString(body)).headers(defaultHeaders()).contentType(APPLICATION_JSON))`
   - `testRestTemplate.getForEntity(url, Type.class)` -> `mockMvc.perform(get(url).headers(defaultHeaders()))`
   - `testRestTemplate.exchange(url, PUT, entity, Type.class)` -> `mockMvc.perform(put(url).content(...).headers(defaultHeaders()))`
4. Convert response assertions:
   - `assertThat(response.getStatusCode()).isEqualTo(OK)` -> `.andExpect(status().isOk())`
   - `assertThat(response.getBody().getField())` -> `.andExpect(jsonPath("$.field").value(...))` or deserialize from `MvcResult`
5. Keep `@MockitoBean` declarations at the class level
6. Keep `@Sql` annotations as-is

#### B. 8 BaseApiControllerTest subclasses (minimal changes)

1. Change `extends BaseApiControllerTest` to `extends BaseTenantIntegrationTest`
2. Replace `wm.baseUrl()` references with `getWiremockUrl()`
3. Already use MockMvc -- no HTTP client migration needed
4. Keep `@MockitoBean`, `@Sql`, WireMock stubs as-is

#### C. 5 BaseKafkaApiTest subclasses (Kafka migration)

1. Change `extends BaseKafkaApiTest` to `extends BaseTenantIntegrationTest`
2. Build `KafkaTemplate` using `KafkaContainerExtension.getBootstrapServers()` instead of `EmbeddedKafkaBroker`
3. Multi-tenant topics (`testing1`, `testing2`, `testing4`) created in each test's `@BeforeAll` via `KafkaContainerExtension.createTopics()`
4. Keep `@MockitoBean InnReachAuthClient` at the class level

#### D. 2 standalone ITs (TenantInitIT, TenantInitWithSystemUserEnabledIT)

1. Change to extend `BaseIntegrationTest` directly (they test tenant init itself)
2. Remove their own WireMock server setup -- use infrastructure from extensions
3. Replace `wm.baseUrl()` with `getWiremockUrl()`
4. Keep `@MockitoBean` declarations and `@TestPropertySource`

#### E. BaseRepositoryTest (18 subclasses) -- scope reduction only

1. Add `@WithPostgresContainer` annotation
2. Remove `@Testcontainers`, `@Container`, `@DynamicPropertySource` boilerplate
3. Change datasource property names to match `PostgresContainerExtension` (`spring.datasource.url`)
4. Keep `@DataJpaTest`, `@AutoConfigureTestDatabase(NONE)`, `@DirtiesContext`, `@ActiveProfiles("test")`
5. Keep inner `BaseRepositoryConfiguration` and `@MockitoBean` fields

### 7. Files Created

```
src/test/java/org/folio/innreach/
  support/
    postgres/
      PostgresContainerExtension.java
      WithPostgresContainer.java
    kafka/
      KafkaContainerExtension.java
      WithKafkaContainer.java
    wiremock/
      WiremockContainerExtension.java
      WiremockStubExtension.java
      WithWiremockContainer.java
      WireMockStub.java
    TestTenantScopedExecutionService.java
  it/
    base/
      BaseIntegrationTest.java
      BaseTenantIntegrationTest.java
```

### 8. Files Deleted

```
src/test/resources/application-it.yml
src/test/java/org/folio/innreach/context/PostgresTestContainersBeanFactoryPostProcessor.java
src/test/java/org/folio/innreach/controller/base/BaseControllerTest.java
src/test/java/org/folio/innreach/controller/base/BaseApiControllerTest.java
src/test/java/org/folio/innreach/domain/listener/base/BaseKafkaApiTest.java
```

### 9. Files Modified

```
pom.xml                                          -- add testcontainers-kafka dependency
src/test/resources/application-test.yml           -- merge it profile, update property placeholders
src/test/java/.../repository/BaseRepositoryTest.java -- use @WithPostgresContainer

# All 39+ test classes re-parented to new base classes
# 26 BaseControllerTest subclasses: TestRestTemplate -> MockMvc conversion
# 8 BaseApiControllerTest subclasses: minimal re-parenting
# 5 BaseKafkaApiTest subclasses: EmbeddedKafka -> Testcontainers Kafka
# 2 standalone ITs: use base class infrastructure
```

### 10. Mocking Strategy

- **Base classes have zero `@MockitoBean` declarations**
- Each test class declares its own `@MockitoBean` fields for the clients it needs mocked
- `@DirtiesContext(AFTER_CLASS)` ensures different mocking configurations don't interfere between classes
- Tests can choose: mock a client via `@MockitoBean` OR stub its HTTP calls via WireMock -- both approaches coexist
- `InnReachAuthClient`: mocked via `@MockitoBean` in the 31 tests that currently mock it; the 8 former `BaseApiControllerTest` tests continue using WireMock stubs for auth
