package org.folio.edge.core.security;

import static org.folio.edge.core.Constants.APPLICATION_JSON;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.ext.web.Router;
import org.apache.http.HttpHeaders;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.edge.core.security.SecureStore.NotFoundException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import software.amazon.awssdk.core.SdkSystemSetting;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.GetParameterRequest;
import software.amazon.awssdk.services.ssm.model.GetParameterResponse;
import software.amazon.awssdk.services.ssm.model.Parameter;
import software.amazon.awssdk.services.ssm.model.ParameterNotFoundException;
import software.amazon.awssdk.services.ssm.model.SsmException;
import software.amazon.awssdk.utils.internal.SystemSettingUtilsTestBackdoor;
import java.net.URISyntaxException;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

@RunWith(VertxUnitRunner.class)
public class AwsParamStoreTest {

  private static final Logger logger = LogManager.getLogger(AwsParamStoreTest.class);

  private static final String MOCK_CREDS = """
      {
        "RoleArn": "arn:aws:iam::0011223344556:role/Role-ecs-task",
        "AccessKeyId": "SOMEBOGUSACCESSKEYID",
        "SecretAccessKey": "ABogusSecretAccessKeyForUnitTestPurposes",
        "Token": "ABogusTokenforUnitTestPurposesABogusTokenforUnitTestPurposes",
        "Expiration": "2099-05-21T20:02:33Z"
      }
      """;

  private static HttpServer server;
  private static AtomicBoolean serverCalled = new AtomicBoolean();

  private static final String ACCESS_KEY_SYSTEM_PROPERTY = SdkSystemSetting.AWS_ACCESS_KEY_ID.property();
  private static final String SECRET_KEY_SYSTEM_PROPERTY = SdkSystemSetting.AWS_SECRET_ACCESS_KEY.property();
  private static final String PROP_AWS_CONTAINER_CREDENTIALS_FULL_URI =
      SdkSystemSetting.AWS_CONTAINER_CREDENTIALS_FULL_URI.property();
  private static String ecsCredEndpoint;
  private static String ecsCredPath = "/v2/credentials/" + UUID.randomUUID();

  private AutoCloseable mocks;

  @Mock
  SsmClient ssm;

  @InjectMocks
  AwsParamStore secureStore;

  @Before
  public void setUp() {
    clearPropertiesAndEnvs();
    // Use empty properties since the only thing configurable
    // is related to AWS, which is mocked here
    Properties props = new Properties();
    props.put(AwsParamStore.PROP_REGION, "us-east-1");
    secureStore = new AwsParamStore(props);
    mocks = MockitoAnnotations.openMocks(this);
    serverCalled.set(false);
  }

  private static void clearPropertiesAndEnvs() {
    System.clearProperty(AwsParamStore.PROP_USE_IAM);
    System.clearProperty(AwsParamStore.PROP_REGION);
    System.clearProperty(AwsParamStore.PROP_ECS_CREDENTIALS_ENDPOINT);
    System.clearProperty(AwsParamStore.PROP_ECS_CREDENTIALS_PATH);
    System.clearProperty(AwsParamStore.PROP_AWS_CONTAINER_CREDENTIALS_RELATIVE_URI);
    System.clearProperty(PROP_AWS_CONTAINER_CREDENTIALS_FULL_URI);
    System.clearProperty(ACCESS_KEY_SYSTEM_PROPERTY);
    System.clearProperty(SECRET_KEY_SYSTEM_PROPERTY);
    SystemSettingUtilsTestBackdoor.clearEnvironmentVariableOverrides();
  }

  @After
  public void tearDown() throws Exception {
    mocks.close();
  }

  @BeforeClass
  public static void setupOnce(TestContext context) {
    // Setup Simple server to host credentials...
    Vertx vertx = Vertx.vertx();
    server = vertx.createHttpServer();

    Router router = Router.router(vertx);
    router.route(HttpMethod.GET, ecsCredPath).handler(ctx -> {
      serverCalled.set(true);
      logger.info("response for GET {}", ecsCredPath);
      ctx.response()
        .setStatusCode(200)
        .putHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON)
        .end(MOCK_CREDS);
    });

    server.requestHandler(router).listen(0)
    .onSuccess(httpServer -> ecsCredEndpoint = "http://localhost:" + httpServer.actualPort())
    .onComplete(context.asyncAssertSuccess());
  }

  @AfterClass
  public static void tearDownOnce(TestContext context) {
    clearPropertiesAndEnvs();
    server.close()
    .onComplete(context.asyncAssertSuccess())
    .onFailure(e -> logger.error("Failed to shut down credentials server", e))
    .onSuccess(x -> logger.info("Successfully shut down credentials server"));
  }

  @Test
  public void testIam() {
    assertEquals("us-east-1", secureStore.getRegion());
  }

  @Test
  public void testConstruction() {
    String euCentral1 = "eu-central-1";
    String useIAM = "false";

    Properties diffProps = new Properties();
    diffProps.setProperty(AwsParamStore.PROP_REGION, euCentral1);
    diffProps.setProperty(AwsParamStore.PROP_USE_IAM, useIAM);
    diffProps.setProperty(AwsParamStore.PROP_ECS_CREDENTIALS_ENDPOINT, "http://example.com");
    diffProps.setProperty(AwsParamStore.PROP_ECS_CREDENTIALS_PATH, "/foo");

    System.setProperty(ACCESS_KEY_SYSTEM_PROPERTY, "bogus");
    System.setProperty(SECRET_KEY_SYSTEM_PROPERTY, "bogus");

    secureStore = new AwsParamStore(diffProps);

    assertEquals(euCentral1, secureStore.getRegion());
    assertEquals(Boolean.parseBoolean(useIAM), secureStore.getUseIAM());
  }

  @Test
  public void testGetFound() throws Exception {
    // test data & expected values
    String clientId = "ditdatdot";
    String tenant = "foo";
    String user = "bar";
    String val = "letmein";
    String key = String.format("%s_%s_%s", clientId, tenant, user);

    // setup mocks/spys/etc.
    GetParameterRequest req = GetParameterRequest.builder().name(key).withDecryption(true).build();
    GetParameterResponse resp = GetParameterResponse.builder()
        .parameter(Parameter.builder().name(key).value(val).build())
        .build();
    when(ssm.getParameter(req)).thenReturn(resp);

    // test & assertions
    assertEquals(val, secureStore.get(clientId, tenant, user));
  }

  @Test
  public void testGetNotFound() {
    String exceptionMsg = "Parameter null_null not found. (Service: AWSSimpleSystemsManagement; Status Code: 400; "
        + "Error Code: ParameterNotFound; Request ID: 25fc4a22-9839-4645-b7b4-ad40aa643821)";
    var exception = ParameterNotFoundException.builder().message(exceptionMsg).build();
    when(ssm.getParameter(any(GetParameterRequest.class))).thenThrow(exception);

    var e = assertThrows(NotFoundException.class, () -> secureStore.get(null, null, null));
    var cause = e.getCause();
    assertTrue(cause.getMessage(), cause.getMessage().contains("25fc4a22-9839-4645-b7b4-ad40aa643821"));
  }

  @Test
  public void testGetFailure() {
    when(ssm.getParameter(any(GetParameterRequest.class))).thenThrow(SdkClientException.create("leap year error"));

    var e = assertThrows(SdkClientException.class, () -> secureStore.get(null, null, null));
    assertEquals("leap year error", e.getMessage());
  }

  @Test
  public void testEnvProvider() {
    System.setProperty(AwsParamStore.PROP_USE_IAM, "false");
    System.setProperty(AwsParamStore.PROP_REGION, "us-east-1");
    SystemSettingUtilsTestBackdoor.addEnvironmentVariableOverride("AWS_ACCESS_KEY_ID", "foo");
    SystemSettingUtilsTestBackdoor.addEnvironmentVariableOverride("AWS_SECRET_ACCESS_KEY", "bar");
    var store = awsParamStore();

    var e = assertThrows(SsmException.class, () -> store.get("clientId", "tenantId", "username"));
    assertTrue(e.getMessage(), e.getMessage().contains("The security token included in the request is invalid."));
    assertFalse(serverCalled.get());
  }

  @Test
  public void testAwsFullUri() {
    System.setProperty(AwsParamStore.PROP_USE_IAM, "false");
    System.setProperty(AwsParamStore.PROP_REGION, "us-east-1");
    System.setProperty(PROP_AWS_CONTAINER_CREDENTIALS_FULL_URI, ecsCredEndpoint + ecsCredPath);
    var store = awsParamStore();

    var e = assertThrows(SsmException.class, () -> store.get("clientId", "tenantId", "username"));
    assertTrue(e.getMessage(), e.getMessage().contains("The security token included in the request is invalid."));
  }

  @Test
  public void testUseEcsCredentialProvider() {
    System.setProperty(AwsParamStore.PROP_USE_IAM, "false");
    System.setProperty(AwsParamStore.PROP_REGION, "us-east-1");
    System.setProperty(AwsParamStore.PROP_ECS_CREDENTIALS_ENDPOINT, ecsCredEndpoint);
    System.setProperty(AwsParamStore.PROP_ECS_CREDENTIALS_PATH, ecsCredPath);
    var store = awsParamStore();
    assertFalse(store.getUseIAM());

    var e = assertThrows(SsmException.class, () -> store.get("clientId", "tenantId", "username"));
    assertTrue(e.getMessage(), e.getMessage().contains("The security token included in the request is invalid."));
    assertTrue(serverCalled.get());
  }

  @Test
  public void testEcsEndpointNull() {
    System.setProperty(AwsParamStore.PROP_USE_IAM, "false");
    System.setProperty(AwsParamStore.PROP_ECS_CREDENTIALS_PATH, "/foo");
    System.setProperty(AwsParamStore.PROP_REGION, "us-east-1");

    var e = assertThrows(SdkClientException.class, () -> awsParamStore());
    assertTrue(e.getMessage(), e.getMessage().contains("AWS_CONTAINER_CREDENTIALS_FULL_URI"));
  }

  @Test
  public void testEcsPathNull() {
    System.setProperty(AwsParamStore.PROP_USE_IAM, "false");
    System.setProperty(AwsParamStore.PROP_ECS_CREDENTIALS_ENDPOINT, "http://example.com");
    System.setProperty(AwsParamStore.PROP_REGION, "us-east-1");

    var e = assertThrows(SdkClientException.class, () -> awsParamStore());
    assertTrue(e.getMessage(), e.getMessage().contains("AWS_CONTAINER_CREDENTIALS_FULL_URI"));
  }

  @Test
  public void testGetCredentialsEndpointURISyntaxException() {
    System.setProperty(AwsParamStore.PROP_USE_IAM, "false");
    System.setProperty(AwsParamStore.PROP_ECS_CREDENTIALS_ENDPOINT, "");
    System.setProperty(AwsParamStore.PROP_ECS_CREDENTIALS_PATH, ":#");

    var e = assertThrows(SdkClientException.class, () -> awsParamStore());
    assertEquals(URISyntaxException.class, e.getCause().getCause().getClass());
  }

  private static AwsParamStore awsParamStore() {
    return new AwsParamStore(System.getProperties());
  }
}
