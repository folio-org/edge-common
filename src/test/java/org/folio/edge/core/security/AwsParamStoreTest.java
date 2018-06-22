package org.folio.edge.core.security;

import static com.amazonaws.SDKGlobalConfiguration.ACCESS_KEY_SYSTEM_PROPERTY;
import static com.amazonaws.SDKGlobalConfiguration.SECRET_KEY_SYSTEM_PROPERTY;
import static org.folio.edge.core.Constants.APPLICATION_JSON;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import java.util.Properties;
import java.util.UUID;

import org.apache.http.HttpHeaders;
import org.apache.log4j.Logger;
import org.folio.edge.core.security.SecureStore.NotFoundException;
import org.folio.edge.core.utils.test.TestUtils;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagement;
import com.amazonaws.services.simplesystemsmanagement.model.AWSSimpleSystemsManagementException;
import com.amazonaws.services.simplesystemsmanagement.model.GetParameterRequest;
import com.amazonaws.services.simplesystemsmanagement.model.GetParameterResult;
import com.amazonaws.services.simplesystemsmanagement.model.Parameter;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;

@RunWith(VertxUnitRunner.class)
public class AwsParamStoreTest {

  private static final Logger logger = Logger.getLogger(AwsParamStoreTest.class);

  private static final String mockCreds = "{\n" +
      "  \"RoleArn\": \"arn:aws:iam::0011223344556:role/Role-ecs-task\",\n" +
      "  \"AccessKeyId\": \"SOMEBOGUSACCESSKEYID\",\n" +
      "  \"SecretAccessKey\": \"ABogusSecretAccessKeyForUnitTestPurposes\",\n" +
      "  \"Token\": \"ABogusTokenforUnitTestPurposesABogusTokenforUnitTestPurposes\",\n" +
      "  \"Expiration\": \"2099-05-21T20:02:33Z\"\n" +
      "}";

  private static HttpServer server;
  private static final int port = TestUtils.getPort();

  private static final String ecsCredEndpoint = "http://localhost:" + port;
  private static final String ecsCredPath = "/v2/credentials/" + UUID.randomUUID();

  @Mock
  AWSSimpleSystemsManagement ssm;

  @InjectMocks
  AwsParamStore secureStore;

  @Before
  public void setUp() throws Exception {
    // Use empty properties since the only thing configurable
    // is related to AWS, which is mocked here
    secureStore = new AwsParamStore(new Properties());

    MockitoAnnotations.initMocks(this);
  }

  @BeforeClass
  public static void setupOnce(TestContext context) throws Exception {
    // Setup Simple server to host credentials...
    Vertx vertx = Vertx.vertx();
    server = vertx.createHttpServer();

    Router router = Router.router(vertx);
    router.route().handler(BodyHandler.create());
    router.route(HttpMethod.GET, ecsCredPath).handler(ctx -> {
      ctx.response()
        .setStatusCode(200)
        .putHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON)
        .end(mockCreds);
    });

    final Async async = context.async();
    server.requestHandler(router::accept).listen(port, result -> {
      if (result.failed()) {
        logger.warn(result.cause());
      }
      context.assertTrue(result.succeeded());
      async.complete();
    });
  }

  @AfterClass
  public static void tearDownOnce() throws Exception {
    server.close(res -> {
      if (res.failed()) {
        logger.error("Failed to shut down credentials server", res.cause());
        fail(res.cause().getMessage());
      } else {
        logger.info("Successfully shut down credentials server");
      }
    });
  }

  @Test
  public void testConstruction() {
    String euCentral1 = "eu-central-1";
    String useIAM = "false";

    Properties diffProps = new Properties();
    diffProps.setProperty(AwsParamStore.PROP_REGION, euCentral1);
    diffProps.setProperty(AwsParamStore.PROP_USE_IAM, useIAM);

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
    GetParameterRequest req = new GetParameterRequest().withName(key).withWithDecryption(true);
    GetParameterResult resp = new GetParameterResult().withParameter(new Parameter().withName(key).withValue(val));
    when(ssm.getParameter(req)).thenReturn(resp);

    // test & assertions
    assertEquals(val, secureStore.get(clientId, tenant, user));
  }

  @Test(expected = NotFoundException.class)
  public void testGetNotFound() throws NotFoundException {
    String exceptionMsg = "Parameter null_null not found. (Service: AWSSimpleSystemsManagement; Status Code: 400; Error Code: ParameterNotFound; Request ID: 25fc4a22-9839-4645-b7b4-ad40aa643821)";
    Throwable exception = new AWSSimpleSystemsManagementException(exceptionMsg);

    when(ssm.getParameter(any())).thenThrow(exception);

    secureStore.get(null, null, null);
  }

  @Test
  public void testUseEcsCredentialProvider() throws Exception {
    Properties properties = new Properties();
    properties.setProperty(AwsParamStore.PROP_USE_IAM, "false");
    properties.setProperty(AwsParamStore.PROP_ECS_CREDENTIALS_ENDPOINT, ecsCredEndpoint);
    properties.setProperty(AwsParamStore.PROP_ECS_CREDENTIALS_PATH, ecsCredPath);

    System.clearProperty(ACCESS_KEY_SYSTEM_PROPERTY);
    System.clearProperty(SECRET_KEY_SYSTEM_PROPERTY);

    AwsParamStore secureStore = new AwsParamStore(properties);
    assertNotNull(secureStore);
    assertFalse(secureStore.getUseIAM());
  }
}
